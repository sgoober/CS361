# CS361

When requesting data, here are the steps:
Make an account on OpenWeatherMap
find your API key on the site, this will be in account information
It accepts city and state parameters
example call:
 api_key = "YOUR_API_KEY"
 city = "Portland"
 state = "OR"
 get_weather(api_key, city, state)

When recieving data, here is what occurs:
The microservice responds with weather data in JSON format
Can create variables that obtain this information, or even translate it through a txt file additionally.

![UML Sequence Diagram](https://github.com/sgoober/CS361/blob/main/seqdiagram.png)
