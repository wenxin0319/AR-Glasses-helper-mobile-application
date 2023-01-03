package com.example.masgserver;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PanZoom;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.example.masgcommunication.Constant;

import java.util.List;

public class ViewLatencyPlotActivity extends Activity {

    XYPlot latencyPlot;

    List<Number> graphData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.latency_plot_graph);
        setResult(Activity.RESULT_CANCELED);

        latencyPlot = findViewById(R.id.latency_plot);

        Bundle extra = getIntent().getExtras();
        graphData = (List<Number>) extra.get(Constant.LATENCY_GRAPH_DATA_KEY);

        plotLatency();
    }

    private void plotLatency() {
        XYSeries series = new SimpleXYSeries(graphData, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED, "plot");
        LineAndPointFormatter formatter = new LineAndPointFormatter(null, Color.BLUE, null, null);
        latencyPlot.addSeries(series, formatter);
        XYGraphWidget graph = latencyPlot.getGraph();
        graph.setPaddingLeft(60);
        graph.setPaddingBottom(30);
        latencyPlot.getLegend().setVisible(false);
        latencyPlot.getOuterLimits().set(0, 300, 0, 2200);
        PanZoom.attach(latencyPlot);
    }
}
