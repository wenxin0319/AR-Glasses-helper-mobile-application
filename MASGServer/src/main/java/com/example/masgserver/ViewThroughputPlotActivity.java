package com.example.masgserver;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.Nullable;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SampledXYSeries;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.example.masgcommunication.Constant;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ViewThroughputPlotActivity extends Activity {
    public List<Number> senderThroughput;
    public List<Number> receiverThroughput;

    XYPlot throughputPlot;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.throughput_plot_graph);
        setResult(Activity.RESULT_CANCELED);

        throughputPlot = findViewById(R.id.throughput_plot);

        Bundle extras = getIntent().getExtras();
        senderThroughput = (List<Number>) extras.getSerializable(Constant.SENDER_THROUGHPUT_KEY);
        receiverThroughput = (List<Number>) extras.getSerializable(Constant.RECEIVER_THROUGHPUT_KEY);

        plotThroughput();
    }

    private void plotThroughput() {
        XYSeries senderSeries = new SimpleXYSeries(senderThroughput, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Sender");
        LineAndPointFormatter senderFormatter = new LineAndPointFormatter(Color.BLUE, null, null, null);
        XYSeries receiverSeries = new SimpleXYSeries(receiverThroughput, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Receiver");
        LineAndPointFormatter receiverFormatter = new LineAndPointFormatter(Color.GREEN, null, null, null);

        throughputPlot.addSeries(senderSeries, senderFormatter);
        throughputPlot.addSeries(receiverSeries, receiverFormatter);
        throughputPlot.getGraph();
    }
}
