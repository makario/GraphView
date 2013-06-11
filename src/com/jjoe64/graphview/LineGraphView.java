package com.jjoe64.graphview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;

import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;

/**
 * Line Graph View. This draws a line chart.
 * @author jjoe64 - jonas gehring - http://www.jjoe64.com
 *
 * Copyright (C) 2011 Jonas Gehring
 * Licensed under the GNU Lesser General Public License (LGPL)
 * http://www.gnu.org/licenses/lgpl.html
 */
public class LineGraphView extends GraphView {

	private final Paint paintBackground = new Paint();
	Path path = new Path();
	private boolean drawBackground;

	private boolean drawBullets;

	private SeriesDrawer seriesDrawer;
	private boolean drawSmoothLine;

	public LineGraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LineGraphView);
		for (int i = 0; i < a.getIndexCount(); ++i) {
			int attr = a.getIndex(i);
			switch (attr) {
			case R.styleable.LineGraphView_drawSeriesPoints:
				setDrawSeriesPoints(a.getBoolean(attr, false));
				break;
			case R.styleable.LineGraphView_seriesPointStyle:
				switch (a.getInt(attr, 0)) {
				case 1:
					seriesDrawer = new SquareSeriesDrawer();
					break;
				case 2:
					seriesDrawer = new TriangleSeriesDrawer();
					break;
				case 0: default:
					seriesDrawer = new CircleSeriesDrawer();
					break;
				}
				break;
			case R.styleable.LineGraphView_drawSmoothLine:
				setDrawSmoothLine(a.getBoolean(attr, drawSmoothLine));
			}
		}
		a.recycle();
	}

	public LineGraphView(Context context) {
		super(context);
		init();
	}

	public void init() {
		paintBackground.setARGB(255, 20, 40, 60);
		paintBackground.setStrokeWidth(4);
	}


	@Override
	public void drawSeries(Canvas canvas, GraphViewData[] values, float graphwidth, float graphheight, float border, double minX, double minY, double diffX, double diffY, float horstart, GraphViewSeriesStyle style) {
		// draw background
		float lastEndY = 0;
		float lastEndX = 0;

		path.reset();

		if (drawBackground) {
			float startY = graphheight + border;
			for (int i = 0; i < values.length; i++) {
				double valY = values[i].valueY - minY;
				double ratY = valY / diffY;
				double y = graphheight * ratY;

				double valX = values[i].valueX - minX;
				double ratX = valX / diffX;
				double x = graphwidth * ratX;

				float endX = (float) x + (horstart + 1);
				float endY = (float) (border - y) + graphheight +2;

				if (i > 0) {
					// fill space between last and current point
					double numSpace = ((endX - lastEndX) / 3f) +1;
					for (int xi=0; xi<numSpace; xi++) {
						float spaceX = (float) (lastEndX + ((endX-lastEndX)*xi/(numSpace-1)));
						float spaceY = (float) (lastEndY + ((endY-lastEndY)*xi/(numSpace-1)));

						// start => bottom edge
						float startX = spaceX;

						// do not draw over the left edge
						if (startX-horstart > 1) {
							canvas.drawLine(startX, startY, spaceX, spaceY, paintBackground);
						}
					}
				}

				lastEndY = endY;
				lastEndX = endX;
			}
		}

		// draw data
		paint.setStrokeWidth(style.thickness);
		paint.setColor(style.color);

		lastEndY = 0;
		lastEndX = 0;

		for (int i = 0; i < values.length; i++) {
			double valY = values[i].valueY - minY;
			double ratY = valY / diffY;
			float y = (float) (graphheight * ratY);

			double valX = values[i].valueX - minX;
			double ratX = valX / diffX;
			float x = (float) (graphwidth * ratX);

			if (i > 0) {
				float startX = (float) lastEndX + (horstart + 1);
				float startY = (float) (border - lastEndY) + graphheight;
				float endX = (float) x + (horstart + 1);
				float endY = (float) (border - y) + graphheight;

				float midX = (startX + endX) / 2;
				float midY = (startY + endY) / 2;

				if (i == 1) {
					path.lineTo(midX, midY);
				} else {
					path.quadTo(startX, startY, midX, midY);
				}

				if (!drawSmoothLine) {
					canvas.drawLine(startX, startY, endX, endY, paint);
				}

				if (drawBullets && seriesDrawer != null) {
					seriesDrawer.drawPoint(canvas, endX, endY, paint);
				}
			} else {
				float xPos = x + (horstart + 1);
				float yPos = (float) ((border - lastEndY) + graphheight);

				if (drawBullets && seriesDrawer != null) {
					seriesDrawer.drawPoint(canvas, xPos, yPos, paint);
				}

				path.moveTo(xPos, yPos);
			}

			lastEndY = y;
			lastEndX = x;
		}

		if (drawSmoothLine) {
			Style oldStyle = paint.getStyle();
			paint.setStyle(Style.STROKE);
			canvas.drawPath(path, paint);
			paint.setStyle(oldStyle);
		}

		// curve.draw(canvas, paint);
	}

	public interface SeriesDrawer {
		void drawPoint(Canvas canvas, float x, float y, Paint paint);
	}

	public class CircleSeriesDrawer implements SeriesDrawer {
		private int radius;

		public CircleSeriesDrawer() {
			this(10);
		}

		public CircleSeriesDrawer(int radius) {
			this.radius = radius;
		}

		@Override
		public void drawPoint(Canvas canvas, float x, float y, Paint paint) {
			canvas.drawCircle(x, y, radius, paint);
		}

		public void setRadius(int radius) {
			this.radius = radius;
		}
	}

	public class TriangleSeriesDrawer implements SeriesDrawer {

		private int size;

		public TriangleSeriesDrawer() {
			this(10);
		}

		public TriangleSeriesDrawer(int radius) {
			this.size = radius;
		}

		@Override
		public void drawPoint(Canvas canvas, float x, float y, Paint paint) {
			Path path = new Path();
			path.moveTo(x, y - size);
			path.lineTo(x + size, y + size);
			path.lineTo(x - size, y + size);
			path.close();
			canvas.drawPath(path, paint);
		}

		public void setRadius(int radius) {
			this.size = radius;
		}
	}


	public class SquareSeriesDrawer implements SeriesDrawer {
		private int size;

		public SquareSeriesDrawer() {
			this(20);
		}

		public SquareSeriesDrawer(int size) {
			this.size = size;
		}

		@Override
		public void drawPoint(Canvas canvas, float x, float y, Paint paint) {
			int halfSize = size/2;
			canvas.drawRect(x - halfSize, y - halfSize, x + halfSize, y + halfSize, paint);
		}

		public void setSize(int size) {
			this.size = size;
		}
	}


	public boolean getDrawBackground() {
		return drawBackground;
	}

	/**
	 * @param drawBackground true for a light blue background under the graph line
	 */
	public void setDrawBackground(boolean drawBackground) {
		this.drawBackground = drawBackground;
	}

	public void setDrawSeriesPoints(boolean drawPoints) {
		this.drawBullets = drawPoints;
		if (drawPoints && seriesDrawer == null) {
			seriesDrawer = new CircleSeriesDrawer();
		}
	}

	public SeriesDrawer getSeriesDrawer() {
		return seriesDrawer;
	}

	public void setSeriesDrawer(SeriesDrawer seriesDrawer) {
		this.seriesDrawer = seriesDrawer;
	}
	
	public void setDrawSmoothLine(boolean drawSmoothLine) {
		this.drawSmoothLine = drawSmoothLine;
	}
}
