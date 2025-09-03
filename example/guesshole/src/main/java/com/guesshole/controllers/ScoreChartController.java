package com.guesshole.controllers;

import com.guesshole.services.GuessService;
import com.guesshole.utils.ScoreCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Controller
public class ScoreChartController {

    private final ConfigurableEnvironment environment;

    @Autowired
    public ScoreChartController(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @GetMapping("/scorechart")
    public Mono<String> showScoreChart(
            @RequestParam(defaultValue = "60") Integer roundDuration,
            @RequestParam(required = false) Double maxDistanceThreshold,
            @RequestParam(required = false) Double minDistanceThreshold,
            @RequestParam(required = false) Double distanceScoreMultiplier,
            @RequestParam(required = false) Double timeScoreMultiplier,
            @RequestParam(required = false) Double graceTimeThreshold,
            Model model) {

        if (maxDistanceThreshold == null) {
            maxDistanceThreshold = environment.getProperty("app.game-round.maxDistanceThreshold", Double.class, 5000.0);
        }
        if (minDistanceThreshold == null) {
            minDistanceThreshold = environment.getProperty("app.game-round.minDistanceThreshold", Double.class, 10.0);
        }
        if (distanceScoreMultiplier == null) {
            distanceScoreMultiplier = environment.getProperty("app.game-round.distanceScoreMultiplier", Double.class, 1.0);
        }
        if (timeScoreMultiplier == null) {
            timeScoreMultiplier = environment.getProperty("app.game-round.timeScoreMultiplier", Double.class, 1.0);
        }
        if (graceTimeThreshold == null) {
            graceTimeThreshold = environment.getProperty("app.game-round.graceTimeThreshold", Double.class, 5.0);
        }

        // Get chart data with current environment settings
        Map<String, Object> chartData = ScoreCalculator.getChartData(
                roundDuration,
                maxDistanceThreshold,
                minDistanceThreshold,
                distanceScoreMultiplier,
                timeScoreMultiplier,
                graceTimeThreshold
        );
        model.addAttribute("chartData", chartData);

        return Mono.just("pages/scorechart");
    }

    @GetMapping(path = "/scorechart/svg", produces = "image/svg+xml")
    @ResponseBody
    public Mono<String> getScoreChartSvg(
            @RequestParam(defaultValue = "60") Integer roundDuration) {

        double maxDistanceThreshold = environment.getProperty("app.game-round.maxDistanceThreshold", Double.class, 5000.0);
        double minDistanceThreshold = environment.getProperty("app.game-round.minDistanceThreshold", Double.class, 10.0);
        double distanceScoreMultiplier = environment.getProperty("app.game-round.distanceScoreMultiplier", Double.class, 1.0);
        double timeScoreMultiplier = environment.getProperty("app.game-round.timeScoreMultiplier", Double.class, 1.0);
        double graceTimeThreshold = environment.getProperty("app.game-round.graceTimeThreshold", Double.class, 5.0);

        // Get chart data
        Map<String, Object> chartData = ScoreCalculator.getChartData(roundDuration, maxDistanceThreshold, minDistanceThreshold, distanceScoreMultiplier, timeScoreMultiplier, graceTimeThreshold);

        // Generate SVG directly without relying on Thymeleaf
        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 800 600\" width=\"800\" height=\"600\">\n");

        // Background
        svg.append("  <rect width=\"800\" height=\"600\" fill=\"#f8f9fa\" />\n");

        // Title
        svg.append("  <text x=\"400\" y=\"30\" font-family=\"Arial\" font-size=\"20\" text-anchor=\"middle\" font-weight=\"bold\">Game Scoring Heatmap (Distance vs Time)</text>\n");

        // Heatmap cells
        svg.append("  <g id=\"heatmap\">\n");

        @SuppressWarnings("unchecked")
        List<List<Integer>> scores = (List<List<Integer>>) chartData.get("scores");
        @SuppressWarnings("unchecked")
        List<Integer> distances = (List<Integer>) chartData.get("distances");
        @SuppressWarnings("unchecked")
        List<Integer> times = (List<Integer>) chartData.get("times");
        int maxScore = (int) chartData.get("maxScore");

        double cellWidth = 600.0 / distances.size();
        double cellHeight = 400.0 / times.size();

        for (int timeIdx = 0; timeIdx < times.size(); timeIdx++) {
            for (int distIdx = 0; distIdx < distances.size(); distIdx++) {
                int score = scores.get(timeIdx).get(distIdx);
                double x = 100 + distIdx * cellWidth;
                double y = 100 + timeIdx * cellHeight;

                // Determine color based on score
                String fillColor;
                if (score == 0) {
                    fillColor = "#f8f9fa";
                } else if (score < maxScore * 0.2) {
                    fillColor = "#cfe2ff";
                } else if (score < maxScore * 0.4) {
                    fillColor = "#9ec5fe";
                } else if (score < maxScore * 0.6) {
                    fillColor = "#6ea8fe";
                } else if (score < maxScore * 0.8) {
                    fillColor = "#3d8bfd";
                } else {
                    fillColor = "#0d6efd";
                }

                svg.append(String.format("    <rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"%s\" stroke=\"#ffffff\" stroke-width=\"0.5\">\n",
                        x, y, cellWidth, cellHeight, fillColor));
                svg.append(String.format("      <title>Distance: %d km\nTime: %d sec\nScore: %d</title>\n",
                        distances.get(distIdx), times.get(timeIdx), score));
                svg.append("    </rect>\n");
            }
        }
        svg.append("  </g>\n");

        // X-axis (Distance)
        svg.append("  <line x1=\"100\" y1=\"500\" x2=\"700\" y2=\"500\" stroke=\"black\" stroke-width=\"2\" />\n");
        svg.append("  <text x=\"400\" y=\"540\" font-family=\"Arial\" font-size=\"16\" text-anchor=\"middle\">Distance (km)</text>\n");

        // X-axis ticks and labels
        for (int i = 0; i < distances.size(); i += 5) {
            if (i < distances.size()) {
                double tickX = 100 + i * cellWidth;
                svg.append(String.format("  <line x1=\"%.1f\" y1=\"500\" x2=\"%.1f\" y2=\"510\" stroke=\"black\" stroke-width=\"2\" />\n",
                        tickX, tickX));
                svg.append(String.format("  <text x=\"%.1f\" y=\"525\" font-family=\"Arial\" font-size=\"12\" text-anchor=\"middle\">%d</text>\n",
                        tickX, distances.get(i)));
            }
        }

        // Y-axis (Time)
        svg.append("  <line x1=\"100\" y1=\"100\" x2=\"100\" y2=\"500\" stroke=\"black\" stroke-width=\"2\" />\n");
        svg.append("  <text x=\"50\" y=\"300\" font-family=\"Arial\" font-size=\"16\" text-anchor=\"middle\" transform=\"rotate(-90, 50, 300)\">Time (seconds)</text>\n");

        // Y-axis ticks and labels
        for (int i = 0; i < times.size(); i++) {
            double tickY = 100 + i * cellHeight;
            svg.append(String.format("  <line x1=\"90\" y1=\"%.1f\" x2=\"100\" y2=\"%.1f\" stroke=\"black\" stroke-width=\"2\" />\n",
                    tickY, tickY));
            svg.append(String.format("  <text x=\"85\" y=\"%.1f\" font-family=\"Arial\" font-size=\"12\" text-anchor=\"end\">%d</text>\n",
                    tickY + 5, times.get(i)));
        }

        // Color scale legend
        svg.append("  <defs>\n");
        svg.append("    <linearGradient id=\"colorScale\" x1=\"0%\" y1=\"0%\" x2=\"100%\" y2=\"0%\">\n");
        svg.append("      <stop offset=\"0%\" stop-color=\"#f8f9fa\" />\n");
        svg.append("      <stop offset=\"20%\" stop-color=\"#cfe2ff\" />\n");
        svg.append("      <stop offset=\"40%\" stop-color=\"#9ec5fe\" />\n");
        svg.append("      <stop offset=\"60%\" stop-color=\"#6ea8fe\" />\n");
        svg.append("      <stop offset=\"80%\" stop-color=\"#3d8bfd\" />\n");
        svg.append("      <stop offset=\"100%\" stop-color=\"#0d6efd\" />\n");
        svg.append("    </linearGradient>\n");
        svg.append("  </defs>\n");

        svg.append("  <rect x=\"650\" y=\"50\" width=\"100\" height=\"20\" fill=\"url(#colorScale)\" />\n");
        svg.append("  <text x=\"650\" y=\"85\" font-family=\"Arial\" font-size=\"12\" text-anchor=\"start\">0</text>\n");
        svg.append(String.format("  <text x=\"750\" y=\"85\" font-family=\"Arial\" font-size=\"12\" text-anchor=\"end\">%d</text>\n", maxScore));
        svg.append("  <text x=\"700\" y=\"100\" font-family=\"Arial\" font-size=\"12\" text-anchor=\"middle\">Score</text>\n");

        svg.append("</svg>");

        return Mono.just(svg.toString());
    }
}