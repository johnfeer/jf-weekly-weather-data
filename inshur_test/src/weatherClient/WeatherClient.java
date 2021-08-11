package weatherClient;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.*;

import javax.swing.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WeatherClient extends JFrame implements ActionListener {
	
	/**
	 * Default SerialVersionUID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Text field to provide instructions to user.
	 */
	private static JTextField banner;

	/**
	 * Text field for user entry.
	 */
	private static JTextField textField; 
    
	/**
	 * Text field to display weather data to user.
	 */
    private static JTextField weatherTextField;
  
    /**
     * Frame to hold panel.
     */
    private static JFrame frame; 
  
    /**
     * Button to send action event for weather data retrieval.
     */
    private static JButton button; 
  
    /**
     * Label used to display error information.
     */
    private static JLabel errorLabel;
    
    /**
     * Panel to hold swing components.
     */
    private static JPanel panel;
     
    /**
     * Client used in sending REST request to OpenWeatherMap.
     */
    private static HttpClient client;
    
    /**
     * API Key required by OpenWeatherMap.
     */
    private static final String API_KEY = "3f159511ac64f5393feaa6f3c700c74b";
	
    /**
     * Initialize instance variables and provide some default values.
     */
	public static void main(String[] args) {
		
		frame = new JFrame("JF Weekly Weather");
		
		errorLabel = new JLabel("");
		
		button = new JButton("GO");
		
		banner = new JTextField("", 34);
		banner.setEditable(false);
		banner.setAlignmentX(CENTER_ALIGNMENT);
		banner.setText("Please enter coordinates in the form \"Latitude, Longitude\"");
		
		weatherTextField = new JTextField("", 46);
		weatherTextField.setEditable(false);
		weatherTextField.setAlignmentX(CENTER_ALIGNMENT);
		
		client = HttpClient.newBuilder().build();
		
		// Instantiate our custom class
		WeatherClient weatherClient = new WeatherClient();
		
		button.addActionListener(weatherClient);
		
		textField = new JTextField("", 24);
		
		panel = new JPanel();
		
		panel.add(banner, BorderLayout.CENTER);
		panel.add(textField, BorderLayout.CENTER);
		panel.add(button);
		panel.add(errorLabel);
		panel.add(weatherTextField, BorderLayout.CENTER);
		
		frame.add(panel);
		
		frame.setSize(600, 150);
		frame.setVisible(true);
		
	}
	
	/**
	 * Handle actions performed by the JButton (should only ever get action command "GO").
	 */
	public void actionPerformed(ActionEvent e) { 
        String s = e.getActionCommand(); 
        
        if (s.equals("GO")) { 
        	
        	errorLabel.setText("");
        	
            List<String> instructions = Arrays.asList(textField.getText().split(", "));
            
            if (instructions.size() != 2) {
            	errorLabel.setText("Please enter a valid latitude/longitude in the correct format");
            	return;
            }
            
            String latitude = instructions.get(0);
            String longitude = instructions.get(1);
            
            try {
            	Double.parseDouble(latitude);
            	Double.parseDouble(longitude);
            } catch(NumberFormatException ex) {
            	errorLabel.setText("Please enter a valid latitude/longitude in the correct format");
            	return;
            }
            
            // Construct the request using user input
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openweathermap.org/data/2.5/onecall?lat="
                    		+ latitude 
                    		+ "&lon="
                    		+ longitude
                    		+ "&exclude=minutely,hourly&appid="
                    		+ API_KEY))
                    .timeout(Duration.ofMinutes(2))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            
            this.sendAndProcessWeeklyWeatherRequest(request);
			
        } 
    }
	
	/**
	 * Retrieve & process weekly weather data given a user-entered lat/long and update the panel.
	 * 
	 * @param request HttpRequest includes lat/long used to retrieve weekly weather data
	 */
	private void sendAndProcessWeeklyWeatherRequest(HttpRequest request) {
		HttpResponse<String> response;
        
		try {
			response = client.send(request, BodyHandlers.ofString());
			
            if (response.statusCode() != 200) {
            	errorLabel.setText(response.body());
            	return;
            }
            
            JSONObject weatherJson = new JSONObject(response.body());
            JSONArray days = (JSONArray) weatherJson.get("daily");
            
            int diff = 0;
            
            double dailyTempMax;
            double weeklyTempMax = 0.0;
            
            int dailyHumidity;
            int weeklyHumidityMax = 0;	
            
            for (int i = 0 ; i < days.length() ; i++) {
            	
            	JSONObject day = (JSONObject) days.get(i);
            	JSONObject dailyTemp = (JSONObject) day.get("temp");
            	dailyTempMax = (Double) dailyTemp.get("max");
            	dailyHumidity = (Integer) day.get("humidity");
            	
            	if (dailyTempMax == weeklyTempMax) {
            		if (dailyHumidity > weeklyHumidityMax) {
            			weeklyHumidityMax = dailyHumidity;
            			diff = i;
            		} 
            	}
            	
            	if (dailyTempMax > weeklyTempMax) {
            		weeklyTempMax = dailyTempMax;
            		weeklyHumidityMax = dailyHumidity;
            		diff = i;
            	}	            	
            }
            
            // Display weekly weather data to user
            weatherTextField.setText("A weekly high of " 
            		+ this.kelvinToFarenheit(weeklyTempMax) 
            		+ " degrees Farenheit with " 
            		+ weeklyHumidityMax 
            		+ "% humidity will occur in " 
            		+ diff 
            		+ " day(s)");
            
		} catch (IOException ex1) {
			weatherTextField.setText("IOException: " + ex1.getMessage());
			ex1.printStackTrace();
		} catch (InterruptedException ex2) {
			weatherTextField.setText("InterruptedException: " + ex2.getMessage());
			ex2.printStackTrace();
		} catch (JSONException ex3) {
			weatherTextField.setText("JSONException: " + ex3.getMessage());
			ex3.printStackTrace();
		}
	}
	
	/**
	 * Convert degrees from Kelvin to Farenheit.
	 * 
	 * @param degreesKelvin temperature in Kelvin
	 * @return String representation of temperature in Farenheit
	 */
	private String kelvinToFarenheit(double degreesKelvin) {
		DecimalFormat df = new DecimalFormat("#.#");
		double degreesFarenheit = (degreesKelvin - 273.15) * 9 / 5 + 32;
		return df.format(degreesFarenheit);
	}
	
}
