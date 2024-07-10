package org.example;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HolidayHandler implements HttpHandler {
    private List<Holiday> holidays; // Pakeista į private kintamąjį
    private final Gson gson;

    public HolidayHandler(List<Holiday> holidays) {
        this.holidays = holidays;
        this.gson = new Gson();
    }

    /**
     * Handles an HTTP request and dispatches it to the appropriate handler method.
     * This method is called when a client sends a request to the server.
     *
     * @param exchange the HTTP exchange object, which represents the request and response
     * @throws IOException if an I/O error occurs while processing the request
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String method = exchange.getRequestMethod();
        handleCORS(exchange);

        switch (method) {
            case "POST" -> {
                switch (uri.getPath()) {
                    case "/createHoliday" -> handleCreateHoliday(exchange);
//                    case "/updateHoliday" -> handleUpdateHoliday(exchange);
//                    case "/deleteHoliday" -> handleDeleteHoliday(exchange);
//                    case "/rateHoliday" -> handleRateHoliday(exchange);
//                    case "/resetRatings" -> handleResetHolidayRatings(exchange);
                    default -> exchange.sendResponseHeaders(404, -1);
                }
            }
            case "GET" -> {
                switch (uri.getPath()) {
                    case "/resetRatings" -> handleResetHolidayRatings(exchange);
                    case "/rateHoliday" -> handleRateHoliday(exchange);
                    case "/deleteHoliday" -> handleDeleteHoliday(exchange);
                    case "/updateHoliday" -> handleUpdateHoliday(exchange);
                    case "/getHolidays" -> handleGetHolidays(exchange);
                    case "/getHoliday" -> handleGetHoliday(exchange);
                    default -> exchange.sendResponseHeaders(404, -1);
                }
            }
            default -> exchange.sendResponseHeaders(404, -1);
        }
    }

    private void handleCORS(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
    }

    /**
     * Handles the rating of a holiday.
     * This method reads the "id" and "rating" parameters from the HTTP request, finds the holiday with the given ID,
     * and updates its ratings. If the rating is out of the 1-5 range or if the ID is invalid, it returns a 400 error.
     * If the holiday is not found, it returns a 404 error.
     *
     * @param exchange the HttpExchange object containing the request and response
     * @throws IOException if an I/O error occurs
     */
    private void handleRateHoliday(HttpExchange exchange) throws IOException {
        Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
        long id;
        int rating;

        // Try to parse "id" and "rating" parameters from the query
        try {
            id = Long.parseLong(params.get("id"));
            rating = Integer.parseInt(params.get("rating"));
        } catch (NumberFormatException e) {
            exchange.sendResponseHeaders(400, -1);
            exchange.getResponseBody().close();
            return;
        }

        // Check if the rating is within the valid range
        if (rating < 1 || rating > 5) {
            exchange.sendResponseHeaders(400, -1);
            exchange.getResponseBody().close();
            return;
        }

        // Find the holiday by ID and update its ratings
        boolean holidayFound = holidays.stream()
                .filter(h -> h.getId() == id)
                .findFirst()
                .map(h -> {
                    int[] newRating = new int[h.getRating().length + 1];
                    System.arraycopy(h.getRating(), 0, newRating, 0, h.getRating().length);
                    newRating[newRating.length - 1] = rating;
                    h.setRating(newRating);
                    h.setAverageRating(RatingCalculator.calculateAverageRating(h.getRating()));
                    saveHolidays();
                    return true;
                })
                .orElse(false);

        // Send appropriate response based on whether the holiday was found
        if (holidayFound) {
            exchange.sendResponseHeaders(200, -1);
        } else {
            exchange.sendResponseHeaders(404, -1);
        }

        // Close the response body
        exchange.getResponseBody().close();
    }

    /**
     * Handles a request to reset a holiday's ratings.
     * This method is called when a client sends a request to reset the ratings of a specific holiday.
     *
     * @param exchange the HTTP exchange object, which represents the request and response
     * @throws IOException if an I/O error occurs while processing the request
     */
    private void handleResetHolidayRatings(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = queryToMap(query);
        long id = Long.parseLong(params.get("id"));
        Holiday holiday = holidays.stream().filter(h -> h.getId() == id).findFirst().orElse(null);
        if (holiday != null) {
            holiday.setRating(new int[0]);
            saveHolidays();
            String response = "Holiday rating has been reset";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void handleGetHoliday(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = queryToMap(query);
        long id = Long.parseLong(params.get("id"));
        Holiday holiday = holidays.stream().filter(h -> h.getId() == id).findFirst().orElse(null);
        if (holiday != null) {
            int[] ratings = holiday.getRating();
            double averageRating = RatingCalculator.calculateAverageRating(ratings);
            holiday.setAverageRating(averageRating);
            String response = gson.toJson(holiday);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }


    private void handleGetHolidays(HttpExchange exchange) throws IOException {
        String response = gson.toJson(holidays);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes(StandardCharsets.UTF_8));
        os.close();
    }



    private void handleDeleteHoliday(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = queryToMap(query);
        long id = Long.parseLong(params.get("id"));
        Holiday holiday = holidays.stream().filter(h -> h.getId() == id).findFirst().orElse(null);
        if (holiday != null) {
            holidays.remove(holiday);
            saveHolidays();
            String response = "Holiday has been deleted successfully";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void handleUpdateHoliday(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = queryToMap(query);

        long id = Long.parseLong(params.get("id"));
        Holiday updatedHoliday = holidays.stream()
                .filter(h -> h.getId() == id)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Holiday not found"));

        updatedHoliday.setTitle(params.get("title"));
        updatedHoliday.setCountry(params.get("country"));
        updatedHoliday.setCity(params.get("city"));
        updatedHoliday.setDuration(params.get("duration"));
        updatedHoliday.setSeason(params.get("season"));
        updatedHoliday.setDescription(params.get("description"));
        updatedHoliday.setPrice(Double.parseDouble(params.get("price")));
        updatedHoliday.setPhotos(params.get("photos").split(","));

        saveHolidays();
        String response = "Holiday has been updated successfully";
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(URLDecoder.decode(entry[0], StandardCharsets.UTF_8), URLDecoder.decode(entry[1], StandardCharsets.UTF_8));
            } else {
                result.put(URLDecoder.decode(entry[0], StandardCharsets.UTF_8), "");
            }
        }
        return result;
    }

    private void saveHolidays() {
        try (FileWriter writer = new FileWriter("holidays.json")) {
            gson.toJson(holidays, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleCreateHoliday(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = queryToMap(query);

        String title = params.get("title");
        String country = params.get("country");
        String city = params.get("city");
        String duration = params.get("duration");
        String season = params.get("season");
        String description = params.get("description");
        double price = Double.parseDouble(params.get("price"));
        String[] photos = params.get("photos").split(",");
        int[] rating = new int[0];

        Holiday holiday = new Holiday(title, country, city, duration, season, description, price, photos, rating);
        holidays.add(holiday);
        saveHolidays();
        String response = "User has been created successfully";
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

}

