package com.jjoe64.graphview;

import java.util.ArrayList;
import java.util.List;

import com.jjoe64.graphview.GraphView.GraphViewData;

public class GraphViewSeries {
	/**
	 * graph series style: color and thickness
	 */
	static public class GraphViewSeriesStyle {
		
		public int color;
		public int thickness;
		private ValueDependentColor valueDependentColor;

		public GraphViewSeriesStyle(int color, int thickness) {
			this.color = color;
			this.thickness = thickness;
		}
		public void setValueDependentColor(ValueDependentColor valueDependentColor) {
			this.valueDependentColor = valueDependentColor;
		}
		public ValueDependentColor getValueDependentColor() {
			return valueDependentColor;
		}
	}

	final String description;
	
	private GraphViewSeriesStyle style;
	GraphViewData[] values;
	private final List<GraphView> graphViews = new ArrayList<GraphView>();
	
	public GraphViewSeries(GraphViewData[] values) {
		this(values, null, null);
	}
	
	public GraphViewSeries(GraphViewData[] values, int color) {
		this(values, null, new GraphViewSeriesStyle(color, 3));
	}

	public GraphViewSeries( GraphViewData[] values, String description, GraphViewSeriesStyle style) {
		this.description = description;
		this.style = style;
		this.values = values;
	}

	/**
	 * this graphview will be redrawn if data changes
	 * @param graphView
	 */
	public void addGraphView(GraphView graphView) {
		this.graphViews.add(graphView);
	}

	/**
	 * add one data to current data
	 * @param value the new data to append
	 * @param scrollToEnd true => graphview will scroll to the end (maxX)
	 */
	public void appendData(GraphViewData value, boolean scrollToEnd) {
		GraphViewData[] newValues = new GraphViewData[values.length + 1];
		int offset = values.length;
		System.arraycopy(values, 0, newValues, 0, offset);

		newValues[values.length] = value;
		values = newValues;
		for (GraphView g : graphViews) {
			if (scrollToEnd) {
				g.scrollToEnd();
			}
		}
	}

	/**
	 * clears the current data and set the new.
	 * redraws the graphview(s)
	 * @param values new data
	 */
	public void resetData(GraphViewData[] values) {
		this.values = values;
		for (GraphView g : graphViews) {
			g.redrawAll();
		}
	}

	public GraphViewSeriesStyle getStyle() {
		return style;
	}

	public void setStyle(GraphViewSeriesStyle style) {
		this.style = style;
	}
}
