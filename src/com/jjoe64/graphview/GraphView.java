package com.jjoe64.graphview;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;

import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.compatible.ScaleGestureDetector;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;

/**
 * GraphView is a Android View for creating zoomable and scrollable graphs.
 * This is the abstract base class for all graphs. Extend this class and implement {@link #drawSeries(Canvas, GraphViewData[], float, float, float, double, double, double, double, float)} to display a custom graph.
 * Use {@link LineGraphView} for creating a line chart.
 *
 * @author jjoe64 - jonas gehring - http://www.jjoe64.com
 *
 * Copyright (C) 2011 Jonas Gehring
 * Licensed under the GNU Lesser General Public License (LGPL)
 * http://www.gnu.org/licenses/lgpl.html
 */
abstract public class GraphView extends LinearLayout {

	static final private class GraphViewConfig {
		static final float BORDER = 20;
		static final float VERTICAL_LABEL_WIDTH = 100;
		static final float HORIZONTAL_LABEL_HEIGHT = 80;
	}

	private class GraphViewContentView extends View {

		private float lastTouchEventX;
		private float graphwidth;

		private boolean drawGrid = true;
		private int gridColor = Color.GRAY;

		public GraphViewContentView(Context context) {
			super(context);
			graphColor = context.getResources().getColor(R.color.holo_blue);
		}


		private Rect textBounds = new Rect();

		@Override
		protected void onDraw(Canvas canvas) {

			paint.setAntiAlias(true);
			paint.setStrokeWidth(0);

			float border = GraphViewConfig.BORDER;
			float horstart = 0;
			float height = getHeight();
			float width = getWidth() - 1;
			double maxY = getMaxY();
			double minY = getMinY();
			double maxX = getMaxX(false);
			double minX = getMinX(false);
			double diffX = maxX - minX;

			// measure bottom text
			int textHeight = 0;
			if (showHorizontalLabels) {
				paint.setTextSize(horizLabelTextSize);
				paint.getTextBounds("a", 0, 1, textBounds);
				textHeight = (int) (textBounds.height() + horizLabelPadding);
			}

			float graphheight = height - (2 * border) - textHeight;
			graphwidth = width;

			if (horlabels == null) {
				horlabels = generateHorlabels(graphwidth);
			}
			if (verlabels == null) {
				verlabels = generateVerlabels(graphheight);
			}

			if (drawGrid) {

				// vertical lines
				paint.setTextAlign(Align.LEFT);
				paint.setColor(gridColor);

				int vers = verlabels.length - 1;
				for (int i = 0; i < verlabels.length; i++) {
					float y = ((graphheight / vers) * i) + border;
					canvas.drawLine(horstart, y, width, y, paint);
				}

				// horizontal labels + lines
				int hors = horlabels.length - 1;
				for (int i = 0; i < horlabels.length; i++) {
					float x = ((graphwidth / hors) * i) + horstart;
					canvas.drawLine(x, height - border - textHeight/2, x, border, paint);

					if (showHorizontalLabels) {
						paint.setTextAlign(Align.CENTER);
						if (i==horlabels.length-1)
							paint.setTextAlign(Align.RIGHT);
						if (i==0)
							paint.setTextAlign(Align.LEFT);

						paint.setColor(horizLabelColor);
						canvas.drawText(horlabels[i], x, height - (textBounds.height() >> 1), paint); // TODO measure text
						paint.setColor(gridColor);
					}
				}
			}

			if (title != null) {
				paint.setTextAlign(Align.CENTER);
				canvas.drawText(title, (graphwidth / 2) + horstart, border - 4, paint);
			}

			if (maxY == minY) {
				// if min/max is the same, fake it so that we can render a line
				maxY = maxY*1.05d;
				minY = minY*0.95d;
			}

			double diffY = maxY - minY;
			paint.setStrokeCap(Paint.Cap.ROUND);

			for (int i=0; i<graphSeries.size(); i++) {
				drawSeries(canvas, _values(i), graphwidth, graphheight, border, minX, minY, diffX, diffY, horstart, graphSeries.get(i).getStyle());
			}

			if (showLegend) drawLegend(canvas, height, width);
		}

		private void onMoveGesture(float f) {
			// view port update
			if (viewportSize != 0) {
				viewportStart -= f*viewportSize/graphwidth;

				// minimal and maximal view limit
				double minX = getMinX(true);
				double maxX = getMaxX(true);
				if (viewportStart < minX) {
					viewportStart = minX;
				} else if (viewportStart+viewportSize > maxX) {
					viewportStart = maxX - viewportSize;
				}

				// labels have to be regenerated
				horlabels = null;
				verlabels = null;
				viewVerLabels.invalidate();
			}
			invalidate();
		}

		/**
		 * @param event
		 */
		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (!isScrollable()) {
				return super.onTouchEvent(event);
			}

			boolean handled = false;
			// first scale
			if (scalable && scaleDetector != null) {
				scaleDetector.onTouchEvent(event);
				handled = scaleDetector.isInProgress();
			}
			if (!handled) {
				// if not scaled, scroll
				if ((event.getAction() & MotionEvent.ACTION_DOWN) == MotionEvent.ACTION_DOWN) {
					handled = true;
				}
				if ((event.getAction() & MotionEvent.ACTION_UP) == MotionEvent.ACTION_UP) {
					lastTouchEventX = 0;
					handled = true;
				}
				if ((event.getAction() & MotionEvent.ACTION_MOVE) == MotionEvent.ACTION_MOVE) {
					if (lastTouchEventX != 0) {
						onMoveGesture(event.getX() - lastTouchEventX);
					}
					lastTouchEventX = event.getX();
					handled = true;
				}
				if (handled)
					invalidate();
			}
			return handled;
		}

		public void setDrawGrid(boolean drawGrid) {
			this.drawGrid = drawGrid;
		}

		public void setGridColor(int gridColor) {
			this.gridColor = gridColor;
		}
	}

	/**
	 * one data set for a graph series
	 */
	static public class GraphViewData {
		public final double valueX;
		public final double valueY;
		public GraphViewData(double valueX, double valueY) {
			this.valueX = valueX;
			this.valueY = valueY;
		}
	}

	public enum LegendAlign {
		TOP, MIDDLE, BOTTOM
	}

	private class VerLabelsView extends View {

		private Rect textBounds = new Rect();
		private Rect vertTextBounds = new Rect();

		/**
		 * @param context
		 */
		public VerLabelsView(Context context) {
			super(context);
			setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 10));
		}

		@Override
		protected void onDraw(Canvas canvas) {

			// normal
			paint.setStrokeWidth(0);

			float border = GraphViewConfig.BORDER;
			float height = getHeight();

			int textHeight = 0;
			if (showHorizontalLabels) {
				paint.getTextBounds("a", 0, 1, textBounds);
				textHeight = (int) (textBounds.height() + horizLabelPadding);
			}


			float graphheight = height - (2 * border) - textHeight;

			if (verlabels == null) {
				verlabels = generateVerlabels(graphheight);
			}

			// vertical labels
			paint.setTextAlign(Align.RIGHT);
			paint.setTextSize(vertLabelTextSize);
			paint.getTextBounds("a", 0, 1, vertTextBounds);
			int vers = verlabels.length - 1;
			for (int i = 0; i < verlabels.length; i++) {
				float y = ((graphheight / vers) * i) + border + (vertTextBounds.height() / 2);
				paint.setColor(vertLabelColor);
				canvas.drawText(verlabels[i], getWidth() - vertLabelPadding, y, paint);
			}
		}

	}

	protected Paint paint = new Paint();
	private String[] horlabels;
	private String[] verlabels;
	private String title;
	private boolean scrollable;
	private double viewportStart;
	private double viewportSize;
	private View viewVerLabels;
	private ScaleGestureDetector scaleDetector;
	private boolean scalable;
	private final NumberFormat[] numberformatter = new NumberFormat[2];
	private List<GraphViewSeries> graphSeries = new ArrayList<GraphViewSeries>();
	private boolean showLegend = false;
	private float legendWidth = 120;
	private LegendAlign legendAlign = LegendAlign.MIDDLE;
	private boolean manualYAxis;
	private double manualMaxYValue;
	private double manualMinYValue;
	private GraphViewContentView graphContentView;

	private int[] graphColors;
	private int nextColor;

	private int graphColor;
	private int vertLabelColor = Color.DKGRAY;
	private int horizLabelColor = Color.DKGRAY;

	private int horizLabelPadding;
	private int vertLabelPadding;

	private int horizLabelTextSize;
	private int vertLabelTextSize;
	private ValueAnimator anim;
	private Interpolator interpolator = new OvershootInterpolator();

	private boolean showVerticalLabels = true;
	private boolean showHorizontalLabels = true;

	public GraphView(Context context) {
		super(context);
		initialize(context);
	}

	public void initialize(Context context) {

		graphColors = new int[] { // these are just ids
				R.color.holo_blue,
				R.color.holo_purple, 
				R.color.holo_green, 
				R.color.holo_orange, 
				R.color.holo_red
		};

		// actually get the colors from resources
		for (int i = 0; i < graphColors.length; i++) {
			graphColors[i] = getResources().getColor(graphColors[i]);
		}

		// defaults
		horizLabelPadding = vertLabelPadding = (int) (8 * getResources().getDisplayMetrics().density);
		horizLabelTextSize = vertLabelTextSize = (int) (14 * getResources().getDisplayMetrics().density);

		viewVerLabels = new VerLabelsView(context);
		graphContentView = new GraphViewContentView(context);

		addView(viewVerLabels);
		addView(graphContentView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1));
	}

	public GraphView(Context context, AttributeSet attrs) {

		super(context, attrs);
		initialize(context);

		if (anim == null) {
			createAnimation();
		}

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GraphView);

		for (int i = 0; i < a.getIndexCount(); ++i) {
			int attr = a.getIndex(i);
			switch (attr) {
			case R.styleable.GraphView_title:
				this.title = a.getString(attr);
				break;
			case R.styleable.GraphView_drawGrid:
				graphContentView.setDrawGrid(a.getBoolean(attr, true));
				break;
			case R.styleable.GraphView_gridColor:
				graphContentView.setGridColor(a.getColor(attr, Color.GRAY));
				break;
			case R.styleable.GraphView_graphColor:
				graphColor = a.getColor(attr, 0);
				break;
			case R.styleable.GraphView_labelColor:
				vertLabelColor = a.getColor(attr, vertLabelColor);
				horizLabelColor = a.getColor(attr, horizLabelColor);
				break;
			case R.styleable.GraphView_labelPadding:
				horizLabelPadding = a.getDimensionPixelSize(attr, horizLabelPadding);
				vertLabelPadding = a.getDimensionPixelSize(attr, vertLabelPadding);
			case R.styleable.GraphView_labelTextSize:
				horizLabelTextSize = a.getDimensionPixelSize(attr, horizLabelTextSize);
				vertLabelTextSize = a.getDimensionPixelSize(attr, vertLabelTextSize);
				break;
			case R.styleable.GraphView_showVerticalLabels:
				setShowVerticalLabels(a.getBoolean(attr, true));
				break;
			case R.styleable.GraphView_showHorizontalLabels:
				setShowHorizontalLabels(a.getBoolean(attr, true));
				break;
			}
		}
		a.recycle();
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setShowVerticalLabels(boolean showVerticalLabels) {
		this.showVerticalLabels = showVerticalLabels;
		viewVerLabels.setVisibility(showVerticalLabels ? View.VISIBLE : View.GONE);
	}

	public void setShowHorizontalLabels(boolean showHorizontalLabels) {
		this.showHorizontalLabels = showHorizontalLabels;
	}

	private GraphViewData[] _values(int idxSeries) {
		GraphViewData[] values = graphSeries.get(idxSeries).values;
		if (viewportStart == 0 && viewportSize == 0) {
			// all data
			return values;
		} else {
			// viewport
			List<GraphViewData> listData = new ArrayList<GraphViewData>();
			for (int i=0; i<values.length; i++) {
				if (values[i].valueX >= viewportStart) {
					if (values[i].valueX > viewportStart+viewportSize) {
						listData.add(values[i]); // one more for nice scrolling
						break;
					} else {
						listData.add(values[i]);
					}
				} else {
					if (listData.isEmpty()) {
						listData.add(values[i]);
					}
					listData.set(0, values[i]); // one before, for nice scrolling
				}
			}
			return listData.toArray(new GraphViewData[listData.size()]);
		}
	}

	public void addSeries(GraphViewSeries series) {
		series.addGraphView(this);

		// if the user didn't specifically set a style, use defaults
		if (series.getStyle() == null) {
			int defaultColor = graphColor != 0 ? graphColor : graphColors[(0 + nextColor++) % graphColors.length];
			series.setStyle(new GraphViewSeriesStyle(defaultColor, 3));
		}

		graphSeries.add(series);
	}

	protected void drawLegend(Canvas canvas, float height, float width) {
		int shapeSize = 15;

		// rect
		paint.setARGB(180, 100, 100, 100);
		float legendHeight = (shapeSize+5)*graphSeries.size() +5;
		float lLeft = width-legendWidth - 10;
		float lTop;
		switch (legendAlign) {
		case TOP:
			lTop = 10;
			break;
		case MIDDLE:
			lTop = height/2 - legendHeight/2;
			break;
		default:
			lTop = height - GraphViewConfig.BORDER - legendHeight -10;
		}
		float lRight = lLeft+legendWidth;
		float lBottom = lTop+legendHeight;
		canvas.drawRoundRect(new RectF(lLeft, lTop, lRight, lBottom), 8, 8, paint);

		for (int i=0; i<graphSeries.size(); i++) {
			paint.setColor(graphSeries.get(i).getStyle().color);
			canvas.drawRect(new RectF(lLeft+5, lTop+5+(i*(shapeSize+5)), lLeft+5+shapeSize, lTop+((i+1)*(shapeSize+5))), paint);
			if (graphSeries.get(i).description != null) {
				paint.setColor(Color.WHITE);
				paint.setTextAlign(Align.LEFT);
				canvas.drawText(graphSeries.get(i).description, lLeft+5+shapeSize+5, lTop+shapeSize+(i*(shapeSize+5)), paint);
			}
		}
	}

	abstract public void drawSeries(Canvas canvas, GraphViewData[] values, float graphwidth, float graphheight, float border, double minX, double minY, double diffX, double diffY, float horstart, GraphViewSeriesStyle style);

	/**
	 * formats the label
	 * can be overwritten
	 * @param value x and y values
	 * @param isValueX if false, value y wants to be formatted
	 * @return value to display
	 */
	protected String formatLabel(double value, boolean isValueX) {
		int i = isValueX ? 1 : 0;
		if (numberformatter[i] == null) {
			numberformatter[i] = NumberFormat.getNumberInstance();
			double highestvalue = isValueX ? getMaxX(false) : getMaxY();
			double lowestvalue = isValueX ? getMinX(false) : getMinY();
			if (highestvalue - lowestvalue < 0.1) {
				numberformatter[i].setMaximumFractionDigits(6);
			} else if (highestvalue - lowestvalue < 1) {
				numberformatter[i].setMaximumFractionDigits(4);
			} else if (highestvalue - lowestvalue < 20) {
				numberformatter[i].setMaximumFractionDigits(3);
			} else if (highestvalue - lowestvalue < 100) {
				numberformatter[i].setMaximumFractionDigits(1);
			} else {
				numberformatter[i].setMaximumFractionDigits(0);
			}
		}
		return numberformatter[i].format(value);
	}

	private String[] generateHorlabels(float graphwidth) {
		int numLabels = (int) (graphwidth/GraphViewConfig.VERTICAL_LABEL_WIDTH);
		String[] labels = new String[numLabels+1];
		double min = getMinX(false);
		double max = getMaxX(false);
		for (int i=0; i<=numLabels; i++) {
			labels[i] = formatLabel(min + ((max-min)*i/numLabels), true);
		}
		return labels;
	}

	private void createAnimation() {
		anim = ValueAnimator.ofFloat(0f, 1f);
		anim.setDuration(1000);

		anim.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator anim) {
				GraphView.this.onAnimationUpdate(anim);
				redrawGraph();
			}
		});

		anim.setInterpolator(interpolator);
		anim.start();
	}

	public void setInterpolator(Interpolator interpolator) {
		this.interpolator = interpolator;
	}

	protected void onAnimationUpdate(ValueAnimator anim) {}

	synchronized private String[] generateVerlabels(float graphheight) {
		int numLabels = (int) (graphheight/GraphViewConfig.HORIZONTAL_LABEL_HEIGHT);
		String[] labels = new String[numLabels+1];
		double min = getMinY();
		double max = getMaxY();
		if (max == min) {
			// if min/max is the same, fake it so that we can render a line
			max = max*1.05d;
			min = min*0.95d;
		}

		for (int i=0; i<=numLabels; i++) {
			labels[numLabels-i] = formatLabel(min + ((max-min)*i/numLabels), false);
		}
		return labels;
	}

	public LegendAlign getLegendAlign() {
		return legendAlign;
	}

	public float getLegendWidth() {
		return legendWidth;
	}

	/**
	 * returns the maximal X value of the current viewport (if viewport is set)
	 * otherwise maximal X value of all data.
	 * @param ignoreViewport
	 *
	 * warning: only override this, if you really know want you're doing!
	 */
	protected double getMaxX(boolean ignoreViewport) {
		// if viewport is set, use this
		if (!ignoreViewport && viewportSize != 0) {
			return viewportStart+viewportSize;
		} else {
			// otherwise use the max x value
			// values must be sorted by x, so the last value has the largest X value
			double highest = 0;
			if (graphSeries.size() > 0)
			{
				GraphViewData[] values = graphSeries.get(0).values;
				if (values.length == 0) {
					highest = 0;
				} else {
					highest = values[values.length-1].valueX;
					for (int i=1; i<graphSeries.size(); i++) {
						values = graphSeries.get(i).values;
						highest = Math.max(highest, values[values.length-1].valueX);
					}
				}
			}
			return highest;
		}
	}

	/**
	 * returns the maximal Y value of all data.
	 *
	 * warning: only override this, if you really know want you're doing!
	 */
	protected double getMaxY() {
		double largest;
		if (manualYAxis) {
			largest = manualMaxYValue;
		} else {
			largest = Integer.MIN_VALUE;
			for (int i=0; i<graphSeries.size(); i++) {
				GraphViewData[] values = _values(i);
				for (int ii=0; ii<values.length; ii++)
					if (values[ii].valueY > largest)
						largest = values[ii].valueY;
			}
		}
		return largest;
	}

	/**
	 * returns the minimal X value of the current viewport (if viewport is set)
	 * otherwise minimal X value of all data.
	 * @param ignoreViewport
	 *
	 * warning: only override this, if you really know want you're doing!
	 */
	protected double getMinX(boolean ignoreViewport) {
		// if viewport is set, use this
		if (!ignoreViewport && viewportSize != 0) {
			return viewportStart;
		} else {
			// otherwise use the min x value
			// values must be sorted by x, so the first value has the smallest X value
			double lowest = 0;
			if (graphSeries.size() > 0)
			{
				GraphViewData[] values = graphSeries.get(0).values;
				if (values.length == 0) {
					lowest = 0;
				} else {
					lowest = values[0].valueX;
					for (int i=1; i<graphSeries.size(); i++) {
						values = graphSeries.get(i).values;
						lowest = Math.min(lowest, values[0].valueX);
					}
				}
			}
			return lowest;
		}
	}

	/**
	 * returns the minimal Y value of all data.
	 *
	 * warning: only override this, if you really know want you're doing!
	 */
	protected double getMinY() {
		double smallest;
		if (manualYAxis) {
			smallest = manualMinYValue;
		} else {
			smallest = Integer.MAX_VALUE;
			for (int i=0; i<graphSeries.size(); i++) {
				GraphViewData[] values = _values(i);
				for (int ii=0; ii<values.length; ii++)
					if (values[ii].valueY < smallest)
						smallest = values[ii].valueY;
			}
		}
		return smallest;
	}

	public boolean isScrollable() {
		return scrollable;
	}

	public boolean isShowLegend() {
		return showLegend;
	}

	public void redrawAll() {
		verlabels = null;
		horlabels = null;
		numberformatter[0] = null;
		numberformatter[1] = null;
		invalidate();
		viewVerLabels.invalidate();
		graphContentView.invalidate();
	}

	public void redrawGraph() {
		graphContentView.invalidate();
		viewVerLabels.invalidate();
	}

	public void removeSeries(GraphViewSeries series)
	{
		graphSeries.remove(series);
	}

	public void removeSeries(int index)
	{
		if (index < 0 || index >= graphSeries.size())
		{
			throw new IndexOutOfBoundsException("No series at index " + index);
		}

		graphSeries.remove(index);
	}

	public void scrollToEnd() {
		if (!scrollable) throw new IllegalStateException("This GraphView is not scrollable.");
		double max = getMaxX(true);
		viewportStart = max-viewportSize;
		redrawAll();
	}

	/**
	 * set's static horizontal labels (from left to right)
	 * @param horlabels if null, labels were generated automatically
	 */
	public void setHorizontalLabels(String[] horlabels) {
		this.horlabels = horlabels;
	}

	public void setLegendAlign(LegendAlign legendAlign) {
		this.legendAlign = legendAlign;
	}

	public void setLegendWidth(float legendWidth) {
		this.legendWidth = legendWidth;
	}

	/**
	 * you have to set the bounds {@link #setManualYAxisBounds(double, double)}. That automatically enables manualYAxis-flag.
	 * if you want to disable the menual y axis, call this method with false.
	 * @param manualYAxis
	 */
	public void setManualYAxis(boolean manualYAxis) {
		this.manualYAxis = manualYAxis;
	}

	/**
	 * set manual Y axis limit
	 * @param max
	 * @param min
	 */
	public void setManualYAxisBounds(double max, double min) {
		manualMaxYValue = max;
		manualMinYValue = min;
		manualYAxis = true;
	}

	/**
	 * this forces scrollable = true
	 * @param scalable
	 */
	synchronized public void setScalable(boolean scalable) {
		this.scalable = scalable;
		if (scalable == true && scaleDetector == null) {
			scrollable = true; // automatically forces this
			scaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
				@Override
				public boolean onScale(ScaleGestureDetector detector) {
					double center = viewportStart + viewportSize / 2;
					viewportSize /= detector.getScaleFactor();
					viewportStart = center - viewportSize / 2;

					// viewportStart must not be < minX
					double minX = getMinX(true);
					if (viewportStart < minX) {
						viewportStart = minX;
					}

					// viewportStart + viewportSize must not be > maxX
					double maxX = getMaxX(true);
					double overlap = viewportStart + viewportSize - maxX;
					if (overlap > 0) {
						// scroll left
						if (viewportStart-overlap > minX) {
							viewportStart -= overlap;
						} else {
							// maximal scale
							viewportStart = minX;
							viewportSize = maxX - viewportStart;
						}
					}
					redrawAll();
					return true;
				}
			});
		}
	}

	/**
	 * the user can scroll (horizontal) the graph. This is only useful if you use a viewport {@link #setViewPort(double, double)} which doesn't displays all data.
	 * @param scrollable
	 */
	public void setScrollable(boolean scrollable) {
		this.scrollable = scrollable;
	}

	public void setShowLegend(boolean showLegend) {
		this.showLegend = showLegend;
	}

	/**
	 * set's static vertical labels (from top to bottom)
	 * @param verlabels if null, labels were generated automatically
	 */
	public void setVerticalLabels(String[] verlabels) {
		this.verlabels = verlabels;
	}

	/**
	 * set's the viewport for the graph.
	 * @param start x-value
	 * @param size
	 */
	public void setViewPort(double start, double size) {
		viewportStart = start;
		viewportSize = size;
	}
}
