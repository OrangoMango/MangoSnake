package com.orangomango.androidbridge;

import com.orangomango.androidbridge.geometry.TextAlignment;
import android.graphics.LinearGradient;

public interface ICanvas {
	void clear(int colorRgb);
	void fillRect(double x, double y, double w, double h, int colorRgb);
	void fillRoundRect(double x, double y, double w, double h, double aw, double ah, int colorRgb);
	void strokeRect(double x, double y, double w, double h, int colorRgb, double lineWidth);
	void strokeRoundRect(double x, double y, double w, double h, double arcWidth, double arcHeight, int rgb, double lw);
	void fillOval(double x, double y, double width, double height, int colorRgb);
	void strokeOval(double x, double y, double w, double h, int colorRgb, double lw);
	void strokeLine(double x1, double y1, double x2, double y2, int colorRgb, double lineWidth);
	void fillPolygon(double[] xPoints, double[] yPoints, int nPoints, int colorRgb);
	void strokePolygon(double[] xPoints, double[] yPoints, int nPoints, int colorRgb, double lineWidth);

	// Text Rendering
	void fillText(String text, double x, double y, int colorRgb, double fontSize, TextAlignment align);

	// Image Rendering (image is a platform-specific Object: Image on Desktop, Bitmap on Android)
	void drawImage(Object image, double dx, double dy, double dw, double dh);
	void drawImage(Object image, double sx, double sy, double sw, double sh, double dx, double dy, double dw, double dh);
	void drawRoundImage(Object image, double dx, double dy, double dw, double dh, double arcWidth, double arcHeight);

	// Transformations
	void save();
	void restore();
	void translate(double x, double y);
	void rotate(double degrees);
	void scale(double x, double y);

	// Effects
	void setEffect(double radius, int rgb);
	void setShader(LinearGradient gradient);
	void setLineDashes(double line, double gap);
	void clearEffect();

	int getWidth();
	int getHeight();

	double measureText(String text, double size);
}