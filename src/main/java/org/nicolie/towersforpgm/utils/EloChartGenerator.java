package org.nicolie.towersforpgm.utils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.nicolie.towersforpgm.rankeds.Rank;

public class EloChartGenerator {

  public static File generateEloChart(
      List<Integer> eloHistory, String username, String outputPath) {
    if (eloHistory == null || eloHistory.isEmpty()) {
      return null;
    }

    try {
      XYSeries series = new XYSeries("ELO");

      series.add(0, 0);

      for (int i = 0; i < eloHistory.size(); i++) {
        series.add(i + 1, eloHistory.get(i)); 
      }

      XYSeriesCollection dataset = new XYSeriesCollection(series);

      JFreeChart chart = ChartFactory.createXYAreaChart(
          null,
          null,
          "ELO",
          dataset,
          PlotOrientation.VERTICAL,
          false, 
          true, 
          false
          );

      customizeChart(chart);

      BufferedImage image = chart.createBufferedImage(800, 600);
      File outputFile = new File(outputPath);
      outputFile.getParentFile().mkdirs();
      ImageIO.write(image, "png", outputFile);

      return outputFile;

    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  private static void customizeChart(JFreeChart chart) {
    XYPlot plot = chart.getXYPlot();

    chart.setBackgroundPaint(new Color(17, 24, 39));
    plot.setBackgroundPaint(new Color(31, 41, 55));

    plot.setOutlineVisible(false);

    plot.setDomainGridlinePaint(new Color(55, 65, 81));
    plot.setRangeGridlinePaint(new Color(55, 65, 81));
    plot.setDomainGridlinesVisible(true);
    plot.setRangeGridlinesVisible(true);

    org.jfree.chart.axis.NumberAxis domainAxis =
        (org.jfree.chart.axis.NumberAxis) plot.getDomainAxis();
    domainAxis.setStandardTickUnits(org.jfree.chart.axis.NumberAxis.createIntegerTickUnits());

    int dataCount = plot.getDataset().getItemCount(0);

    float lineWidth;
    double pointSize;
    boolean showPoints;

    if (dataCount > 200) {
      lineWidth = 2.5f;
      pointSize = 0;
      showPoints = false;
    } else if (dataCount > 100) {
      lineWidth = 3.0f;
      pointSize = 4;
      showPoints = true;
    } else if (dataCount > 50) {
      lineWidth = 3.5f;
      pointSize = 5;
      showPoints = true;
    } else {
      lineWidth = 4.5f;
      pointSize = 6;
      showPoints = true;
    }

    XYSplineRenderer renderer = new XYSplineRenderer();
    renderer.setPrecision(10);
    Color mainColor = new Color(129, 140, 248);

    renderer.setSeriesPaint(0, mainColor);
    renderer.setSeriesFillPaint(0, new Color(99, 102, 241, 120));

    renderer.setSeriesStroke(
        0,
        new java.awt.BasicStroke(
            lineWidth, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));

    if (showPoints) {
      renderer.setSeriesShape(
          0,
          new java.awt.geom.Ellipse2D.Double(-pointSize / 2, -pointSize / 2, pointSize, pointSize));
      renderer.setSeriesShapesFilled(0, true);
      renderer.setSeriesShapesVisible(0, true);
    } else {
      renderer.setSeriesShapesVisible(0, false);
    }

    plot.setRenderer(renderer);

    addRankBands(plot);

    java.awt.Font labelFont = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14);
    plot.getDomainAxis().setLabelFont(labelFont);
    plot.getRangeAxis().setLabelFont(labelFont);
    plot.getDomainAxis().setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
    plot.getRangeAxis().setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));

    plot.getDomainAxis().setTickLabelPaint(new Color(209, 213, 219));
    plot.getRangeAxis().setTickLabelPaint(new Color(209, 213, 219));
    plot.getRangeAxis().setLabelPaint(new Color(229, 231, 235));
  }

  private static void addRankBands(XYPlot plot) {
    Color backgroundColor = new Color(31, 41, 55); 

    for (Rank rank : Rank.values()) {
      Color rankColor = rank.getEmbedColor();
      Color subtleColor = blendColors(backgroundColor, rankColor, 0.15f);

      IntervalMarker marker = new IntervalMarker(rank.getMinElo(), rank.getMaxElo());
      marker.setPaint(subtleColor);
      marker.setAlpha(0.3f);

      plot.addRangeMarker(marker, Layer.BACKGROUND);
    }
  }

  private static Color blendColors(Color base, Color blend, float ratio) {
    float inverseRatio = 1.0f - ratio;
    int r = (int) (base.getRed() * inverseRatio + blend.getRed() * ratio);
    int g = (int) (base.getGreen() * inverseRatio + blend.getGreen() * ratio);
    int b = (int) (base.getBlue() * inverseRatio + blend.getBlue() * ratio);
    return new Color(r, g, b);
  }
}
