import requests

def get_weather(api_key, city, state):
    base_url = "http://api.openweathermap.org/data/2.5/weather"
    # This can be switched to suit your program. You can obtain this in yours and then pass it to the micro service.
    location = f"{city},{state},US"
    params = {"q": location, "appid": api_key, "units": "imperial"}  # Use "metric" for Celsius

    try:
        response = requests.get(base_url, params=params)
        data = response.json()

        if response.status_code == 200:
            temperature = data["main"]["temp"]
            description = data["weather"][0]["description"]
            print(f"Weather in {city}, {state}: {temperature}°F, {description}")
        else:
            print(f"Error: {data['message']}")

    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == "__main__":
    # Replace 'YOUR_API_KEY' with the API key you obtained from OpenWeatherMap (this is currently under mine)
    api_key = "438973b67eb9be6060176dfd37ae65a6"
    city = input("Enter the city name: ")
    state = input("Enter the state abbreviation (e.g., CA): ")
    get_weather(api_key, city, state)
