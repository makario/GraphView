package com.jjoe64.graphview;

import java.util.Arrays;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;

import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;

/**
 * Draws a Bar Chart
 * @author Muhammad Shahab Hameed
 */
public class BarGraphView extends GraphView {

	private float scaleY = 1;
	private float barWidth;
	private float maxBarWidth = Integer.MAX_VALUE;

	private int index;
	private float[][] yScales;
	private boolean animateChildrenSeparately;

	public BarGraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BarGraphView);
		for (int i = 0; i < a.getIndexCount(); ++i) {
			int attr = a.getIndex(i);
			switch (attr) {
			case R.styleable.BarGraphView_barWidth:
				setBarWidth(a.getDimensionPixelSize(attr, (int) barWidth));
				break;
			case R.styleable.BarGraphView_maxBarWidth:
				setMaxBarWidth(a.getDimensionPixelSize(attr, (int) maxBarWidth));
			}
		}
		a.recycle();
	}

	public BarGraphView(Context context) {
		super(context);
	}

	@Override
	protected void drawSeries(Canvas canvas, float border, float horstart, double minY, double minX, double diffX, float graphheight, float graphwidth, double diffY) {
		for (int i = 0; i<graphSeries.size(); i++) {
			index = i;
			drawDataSeries(canvas, _values(i), graphwidth, graphheight, border, minX, minY, diffX, diffY, horstart, graphSeries.get(i).getStyle());
		}
	}

	@Override
	public void drawDataSeries(Canvas canvas, GraphViewData[] values, float graphwidth, float graphheight, float border, double minX, double minY, double diffX, double diffY, float horstart, GraphViewSeriesStyle style) {

		//float colwidth = (graphwidth - (2 * border)) / (values.length);
		// float colwidth = (graphwidth - (0 * border)) / (values.length - 2);
		float colwidth = Math.min((graphwidth - (0 * border)) / (values.length), maxBarWidth);

		float w = barWidth > 0 ? barWidth : colwidth - 1;

		paint.setStrokeWidth(style.thickness);
		paint.setColor(style.color);

		// draw data
		for (int i = 0; i < values.length; i++) {
			float valY = (float) (values[i].valueY - minY);
			float ratY = (float) (valY / diffY);

			// float y = graphheight * ratY * scaleY;
			float y = graphheight * ratY * yScales[index][i];

			// hook for value dependent color
			if (style.getValueDependentColor() != null) {
				paint.setColor(style.getValueDependentColor().get(values[i]));
			}

			canvas.drawRect((i * colwidth) + horstart + colwidth/2 - w/2 , (border - y) + graphheight, ((i * colwidth) + horstart) + colwidth/2 + w - w/2, graphheight + border, paint);
			// canvas.drawRect((i * colwidth) + horstart, (border - y) + graphheight, ((i * colwidth) + horstart) + (colwidth - 1), graphheight + border - 1, paint);
		}
	}

	@Override
	protected void onAnimationUpdate(ValueAnimator anim) {
		scaleY = (Float) anim.getAnimatedValue();
		if (!animateChildrenSeparately) {
			for (float[] y : yScales) {
				Arrays.fill(y, scaleY);
			}
		}
	}

	public void setBarWidth(float barWidth) {
		this.barWidth = barWidth;
	}

	public void setMaxBarWidth(float maxBarWidth) {
		this.maxBarWidth = maxBarWidth;
	}

	@Override
	public void addSeries(GraphViewSeries series) {
		super.addSeries(series);
		yScales = new float[graphSeries.size()][];
		for (int i = 0; i < graphSeries.size(); i++) {
			yScales[i] = new float[graphSeries.get(i).values.length];
			Arrays.fill(yScales[i], 1);
		}
	}

	@Override
	public void startAnimation() {
		if (animateChildrenSeparately) {
			ValueAnimator anim = super.createAnimation();
			for (int i = 0; i < graphSeries.size(); i++) {
				for (int j = 0; j < graphSeries.get(i).values.length; j++) {

					final int x = i;
					final int y = j;

					final ValueAnimator a = anim.clone();
					yScales[x][y] = 0;
					a.addUpdateListener(new AnimatorUpdateListener() {
						@Override
						public void onAnimationUpdate(ValueAnimator arg0) {
							try {
								yScales[x][y] = (Float) a.getAnimatedValue();
								redrawAll();
							} catch (Exception e) {

							}
						}
					});
					a.setStartDelay(j*50);
					a.start();
				}
			}
		} else {
			super.startAnimation();
		}
	}

	public void setAnimateChildrenSeparately(boolean animateChildrenSeparately) {
		this.animateChildrenSeparately = animateChildrenSeparately;
	}
}
