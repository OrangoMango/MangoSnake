package com.orangomango.snake;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import android.app.Activity;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Consumer;

import static com.orangomango.snake.screen.HomeScreen.PURCHASE_HANDLER;
import static com.orangomango.snake.screen.HomeScreen.PurchaseHandler.MANGOCOINS_BUNDLE;

public class BillingManager{
	private final BillingClient billingClient;
	private final Activity activity;
	private Runnable onDone;
	private ArrayList<String> pendingList = new ArrayList<String>();

	private final PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
		if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null){
			for (Purchase purchase : purchases){
				handlePurchase(purchase);
			}
		}
	};

	public BillingManager(Activity activity){
		this.activity = activity;

		this.billingClient = BillingClient.newBuilder(activity)
				.setListener(purchasesUpdatedListener)
				.enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
				.enableAutoServiceReconnection()
				.build();

		startConnection();
	}

	private void startConnection(){
		this.billingClient.startConnection(new BillingClientStateListener(){
			@Override
			public void onBillingSetupFinished(BillingResult billingResult){
				if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
					checkPurchaseDone();
				}
			}
			@Override public void onBillingServiceDisconnected() {}
		});
	}

	private void checkPurchaseDone(){
		QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
				.setProductType(BillingClient.ProductType.INAPP)
				.build();

		this.billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
			if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
				for (Purchase purchase : purchases) {
					if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED){
						String id = purchase.getProducts().get(0); // The user owns the item already
						this.pendingList.add(id);
					}
				}
			}
		});
	}

	public void emptyPendingList(){
		for (String id : this.pendingList){
			this.onDone = () -> Log.d("BILLING", "Restoring purchase: "+id);
			unlockContent(id);
		}
		this.pendingList.clear();
	}

	public void getProductDetails(String productId, Consumer<ProductDetails> consumer){
		QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
				.setProductList(Collections.singletonList(
						QueryProductDetailsParams.Product.newBuilder()
								.setProductId(productId)
								.setProductType(BillingClient.ProductType.INAPP)
								.build()
				)).build();

		this.billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
			if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && !productDetailsList.getProductDetailsList().isEmpty()){
				ProductDetails currentProductDetails = productDetailsList.getProductDetailsList().get(0);
				consumer.accept(currentProductDetails);
			}
		});
	}

	public void purchaseProduct(String productId, Runnable onDone){
		QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
				.setProductList(Collections.singletonList(
						QueryProductDetailsParams.Product.newBuilder()
								.setProductId(productId)
								.setProductType(BillingClient.ProductType.INAPP)
								.build()
				)).build();

		this.onDone = onDone;

		this.billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
			if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && !productDetailsList.getProductDetailsList().isEmpty()){
				ProductDetails currentProductDetails = productDetailsList.getProductDetailsList().get(0);
				launchFlow(currentProductDetails);
			}
		});
	}

	private void launchFlow(ProductDetails currentProductDetails){
		BillingFlowParams flowParams = BillingFlowParams.newBuilder()
				.setProductDetailsParamsList(Collections.singletonList(
						BillingFlowParams.ProductDetailsParams.newBuilder()
								.setProductDetails(currentProductDetails)
								.build()
				)).build();

		this.billingClient.launchBillingFlow(this.activity, flowParams);
	}

	private void handlePurchase(Purchase purchase){
		if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED){
			String productId = purchase.getProducts().get(0);

			if (productId.equals(MANGOCOINS_BUNDLE)){ // Hard code a consumable purchase for now, TODO: change this in future
				ConsumeParams consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();

				this.billingClient.consumeAsync(consumeParams, (billingResult, purchaseToken) -> {
					if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
						unlockContent(productId);
					}
				});
			} else {
				AcknowledgePurchaseParams ackParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();

				this.billingClient.acknowledgePurchase(ackParams, billingResult -> {
					if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
						unlockContent(productId);
					}
				});
			}
		}
	}

	private void unlockContent(String productId){
		PURCHASE_HANDLER.handlePurchase(productId);
		this.onDone.run();
		this.onDone = null;
	}
}