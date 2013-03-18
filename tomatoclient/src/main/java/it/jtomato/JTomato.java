package it.jtomato;

import it.jtomato.gson.AbridgedCast;
import it.jtomato.gson.Movie;
import it.jtomato.gson.Review;
import it.jtomato.net.NetHttpClient;
import it.jtomato.net.NetHttpClientInterface;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * This is the client for the <a href="http://developer.rottentomatoes.com"
 * target="_blank"> Rotten Tomatoes API</a> To use it is necessary to have a
 * valid API key.
 * 
 * @author <a href="mailto:tambug@gmail.com">Giordano Tamburrelli</a>
 * 
 * @version 1.0
 **/
public class JTomato {

	private String key;
	private String page_limit = "30";
	private NetHttpClientInterface httpClient;
	private Gson gson = new Gson();

	/**
	 * Creates a new JTomato instance with a specific HTTP client.
	 * 
	 * @param key
	 *            the api key for the Rotten Tomatoes API. A valid key can be
	 *            obtained at <a href="http://developer.rottentomatoes.com"
	 *            target="_blank"> Rotten Tomatoes API</a>
	 * 
	 * @param httpClient
	 *            A {@link NetHttpClientInterface} used to issue HTTP requests
	 *            to the Rotten Tomatoes API server.
	 * 
	 * 
	 * @see NetHttpClientInterface
	 */
	public JTomato(String key, NetHttpClientInterface httpClient) {
		this.httpClient = httpClient;
		this.key = key;

	}

	/**
	 * Creates a new JTomato instance.
	 * 
	 * @param key
	 *            the api key for the Rotten Tomatoes API. A valid key can be
	 *            obtained at <a href="http://developer.rottentomatoes.com"
	 *            target="_blank"> Rotten Tomatoes API</a>
	 * 
	 */
	public JTomato(String key) {
		this.httpClient = new NetHttpClient();
		this.key = key;

	}

	private HashMap<String, String> getParamsMap() {
		HashMap<String, String> paramsMap = new HashMap<String, String>();
		paramsMap.put("apikey", key);
		return paramsMap;
	}

	/**
	 * The movies search endpoint for plain text queries. Allows you to search
	 * for movies. Results are paginated if they go past the specified page
	 * limit.
	 * 
	 * @param query
	 *            The plain text search query to search for a movie. Encoding is
	 *            performed internally. The number of results per page can be
	 *            set with the {@link #setPage_limit(int)} method.
	 * 
	 * @param result
	 *            A List of {@link Movie} objects. The movies obtained invoking
	 *            this method are appended to this list. Notice that the movies
	 *            obtained with this invocation do not have ids (the id property
	 *            is null).
	 * 
	 * @param page
	 *            The selected page of movie search results.
	 * 
	 * @return The total number of movies matching the query. This value
	 *         combined to the page limit ({@link #setPage_limit(int)}) may be
	 *         used to compute the number of pages that contains the total
	 *         results.
	 * 
	 * @see Movie
	 * @see <a
	 *      href="http://developer.rottentomatoes.com/docs/read/json/v10/Movies_Search"
	 *      target="_blank"> Rotten Tomatoes API Documentation: Movies
	 *      Search</a>
	 */
	public int searchMovie(String query, List<Movie> result, int page) {
		HashMap<String, String> paramsMap = getParamsMap();
		paramsMap.put("q", query);
		paramsMap.put("page_limit", page_limit);
		paramsMap.put("page", String.valueOf(page));
		int total = 0;

		try {
			URI uri = httpClient.buildURI(null, urls.ROTTENTOMATOES_API, urls.SEARCH_MOVIES, paramsMap);
			String response = httpClient.get(uri);
			JsonParser parser = new JsonParser();
			JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
			JsonArray movies = jsonResponse.get("movies").getAsJsonArray();

			total = jsonResponse.get("total").getAsInt();
			for (int i = 0; i < movies.size(); i++) {
				JsonObject movieJson = movies.get(i).getAsJsonObject();

				Movie movie = parseMovieJson(movieJson);
				if (movie != null) {
					result.add(movie);
				}
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		return total;
	}

	/**
	 * Returns Top Box Office Earning Movies, Sorted by Most Recent Weekend
	 * Gross Ticket Sales.
	 * 
	 * @param country
	 *            Provides localized data for the selected country <a
	 *            href="http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2"
	 *            target="_blank"> (ISO 3166-1 alpha-2)</a> if available.
	 *            Otherwise, returns US data.
	 * 
	 * @param limit
	 *            Limits the number of box office movies returned. The maximum
	 *            value is 50, a request with a greater value is considered as
	 *            if it were issued with a limit equal to 50 . This parameter is
	 *            optional, it can be set to a negative value or zero value if
	 *            no limit is required.
	 * 
	 * @return A List of {@link Movie} objects
	 * 
	 * @see Movie
	 * @see <a
	 *      href="http://developer.rottentomatoes.com/docs/read/json/v10/Box_Office_Movies"
	 *      target="_blank"> Rotten Tomatoes API Documentation: Box Office
	 *      Movies</a>
	 */
	public List<Movie> getBoxOfficeMovies(String country, int limit) {
		List<Movie> movies = new ArrayList<Movie>();
		try {
			movies = getNonPaginatedResults(urls.BOX_OFFICE_MOVIES, limit, country);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return movies;
	}

	/**
	 * Returns movies currently in theaters. Results are paginated if they go
	 * past the specified page limit.
	 * 
	 * @param result
	 *            A List of {@link Movie} objects. The movies obtained invoking
	 *            this method are appended to this list.
	 * 
	 * @param country
	 *            Provides localized data for the selected country <a
	 *            href="http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2"
	 *            target="_blank"> (ISO 3166-1 alpha-2)</a> if available. If
	 *            this parameter is null, the method returns US data.
	 * 
	 * @param page
	 *            The selected page of in theaters movies.
	 * 
	 * @return The total number of movies in theaters. This value combined to
	 *         the page limit ({@link #setPage_limit(int)}) may be used to
	 *         compute the number of pages that contains the total results.
	 * 
	 * @see Movie
	 * @see <a
	 *      href="http://developer.rottentomatoes.com/docs/read/json/v10/In_Theaters_Movies"
	 *      target="_blank"> Rotten Tomatoes API Documentation: In Theaters
	 *      Movies</a>
	 */
	public int getInThreatersMovies(List<Movie> result, String country, int page) {
		int total = 0;
		try {
			total = getPaginatedResults(urls.IN_THEATHERS_MOVIES, result, page, country);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return total;
	}

	/**
	 * Returns current opening movies.
	 * 
	 * @param country
	 *            Provides localized data for the selected country <a
	 *            href="http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2"
	 *            target="_blank"> (ISO 3166-1 alpha-2)</a> if available.
	 *            Otherwise, returns US data.
	 * 
	 * @param limit
	 *            Limits the number of opening movies returned. The maximum
	 *            value is 50, a request with a greater value is considered as
	 *            if it were issued with a limit equal to 50 . This parameter is
	 *            optional, it can be set to a negative value or zero value if
	 *            no limit is required.
	 * 
	 * @return A List of {@link Movie} objects
	 * 
	 * @see Movie
	 * @see <a
	 *      href="http://developer.rottentomatoes.com/docs/read/json/v10/Opening_Movies"
	 *      target="_blank"> Rotten Tomatoes API Documentation: Opening
	 *      Movies</a>
	 */
	public List<Movie> getOpeningMovies(String country, int limit) {
		List<Movie> movies = new ArrayList<Movie>();
		try {
			movies = getNonPaginatedResults(urls.OPENING_MOVIES, limit, country);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return movies;
	}

	/**
	 * Returns upcoming movies. Results are paginated if they go past the
	 * specified page limit.
	 * 
	 * @param result
	 *            A List of {@link Movie} objects. The movies obtained invoking
	 *            this method are appended to this list.
	 * 
	 * @param country
	 *            Provides localized data for the selected country <a
	 *            href="http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2"
	 *            target="_blank"> (ISO 3166-1 alpha-2)</a> if available. If
	 *            this parameter is null, the method returns US data.
	 * 
	 * @param page
	 *            The selected page of upcoming movies.
	 * 
	 * @return The total number of upcoming movies. This value combined to the
	 *         page limit ({@link #setPage_limit(int)}) may be used to compute
	 *         the number of pages that contains the total results.
	 * 
	 * @see Movie
	 * @see <a
	 *      href="http://developer.rottentomatoes.com/docs/read/json/v10/Upcoming_Movies"
	 *      target="_blank"> Rotten Tomatoes API Documentation: Upcoming
	 *      Movies</a>
	 */
	public int getUpcomingMovies(List<Movie> result, String country, int page) {
		int total = 0;
		try {
			total = getPaginatedResults(urls.UPCOMING_MOVIES, result, page, country);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return total;
	}

	/**
	 * Returns the current top dvd rentals.
	 * 
	 * @param country
	 *            Provides localized data for the selected country <a
	 *            href="http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2"
	 *            target="_blank"> (ISO 3166-1 alpha-2)</a> if available.
	 *            Otherwise, returns US data.
	 * 
	 * @param limit
	 *            Limits the number of top dvd rentals returned. The maximum
	 *            value is 50, a request with a greater value is considered as
	 *            if it were issued with a limit equal to 50 . This parameter is
	 *            optional, it can be set to a negative value or zero value if
	 *            no limit is required.
	 * 
	 * @return A List of {@link Movie} objects
	 * 
	 * @see Movie
	 * @see <a
	 *      href="http://developer.rottentomatoes.com/docs/read/json/v10/Top_Rentals"
	 *      target="_blank"> Rotten Tomatoes API Documentation: Top Rentals</a>
	 */
	public List<Movie> getTopRentalsMovies(String country, int limit) {
		List<Movie> movies = new ArrayList<Movie>();
		try {
			movies = getNonPaginatedResults(urls.TOP_RENTALS_MOVIES, limit, country);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return movies;
	}

	/**
	 * Returns current release dvds. Results are paginated if they go past the
	 * specified page limit.
	 * 
	 * @param result
	 *            A List of {@link Movie} objects. The movies obtained invoking
	 *            this method are appended to this list.
	 * 
	 * @param country
	 *            Provides localized data for the selected country <a
	 *            href="http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2"
	 *            target="_blank"> (ISO 3166-1 alpha-2)</a> if available. If
	 *            this parameter is null, the method returns US data.
	 * 
	 * @param page
	 *            The selected page of current released dvds.
	 * 
	 * @return The total number of movies currently released in dvd. This value
	 *         combined to the page limit ({@link #setPage_limit(int)}) may be
	 *         used to compute the number of pages that contains the total
	 *         results.
	 * 
	 * @see Movie
	 * @see <a
	 *      href="http://developer.rottentomatoes.com/docs/read/json/v10/Current_Release_DVDs"
	 *      target="_blank"> Rotten Tomatoes API Documentation: Current Release
	 *      DVDs</a>
	 */
	public int getCurrentReleaseDvds(List<Movie> result, String country, int page) {
		int total = 0;
		try {
			total = getPaginatedResults(urls.CURRENT_RELEASE_DVD, result, page, country);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return total;
	}

	/**
	 * Returns new release dvds. Results are paginated if they go past the
	 * specified page limit.
	 * 
	 * @param result
	 *            A List of {@link Movie} objects. The movies obtained invoking
	 *            this method are appended to this list.
	 * 
	 * @param country
	 *            Provides localized data for the selected country <a
	 *            href="http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2"
	 *            target="_blank"> (ISO 3166-1 alpha-2)</a> if available. If
	 *            this parameter is null, the method returns US data.
	 * 
	 * @param page
	 *            The selected page of new released dvds.
	 * 
	 * @return The total number of new movies released in dvd. This value
	 *         combined to the page limit ({@link #setPage_limit(int)}) may be
	 *         used to compute the number of pages that contains the total
	 *         results.
	 * 
	 * @see Movie
	 * @see <a
	 *      href="http://developer.rottentomatoes.com/docs/read/json/v10/New_Release_DVDs"
	 *      target="_blank"> Rotten Tomatoes API Documentation: New Release
	 *      DVDs</a>
	 */
	public int getNewReleaseDvds(List<Movie> result, String country, int page) {
		int total = 0;
		try {
			total = getPaginatedResults(urls.NEW_RELEASE_DVD, result, page, country);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return total;
	}

	/**
	 * Returns upcoming movies. Results are paginated if they go past the
	 * specified page limit.
	 * 
	 * @param result
	 *            A List of {@link Movie} objects. The movies obtained invoking
	 *            this method are appended to this list.
	 * 
	 * @param country
	 *            Provides localized data for the selected country <a
	 *            href="http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2"
	 *            target="_blank"> (ISO 3166-1 alpha-2)</a> if available. If
	 *            this parameter is null, the method returns US data.
	 * 
	 * @param page
	 *            The selected page of upcoming dvds.
	 * 
	 * @return The total number of upcoming movies in dvd. This value combined
	 *         to the page limit ({@link #setPage_limit(int)}) may be used to
	 *         compute the number of pages that contains the total results.
	 * 
	 * @see Movie
	 * @see <a
	 *      href="http://developer.rottentomatoes.com/docs/read/json/v10/Upcoming_Movies"
	 *      target="_blank"> Rotten Tomatoes API Documentation: Upcoming
	 *      Movies</a>
	 */
	public int getUpcomingDvds(List<Movie> result, String country, int page) {
		int total = 0;
		try {
			total = getPaginatedResults(urls.UPCOMING_DVD, result, page, country);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return total;
	}

	/**
	 * Returns detailed information on a specific {@link Movie} object. You can
	 * use the movies search endpoint or peruse the lists of movies/dvds to get
	 * the movies objects.
	 * 
	 * @param movie
	 *            A {@link Movie} object containing a valid movie id or a valid
	 *            self-link in {@link Movie#links}. If both of them are non-null
	 *            the method uses the id.
	 * 
	 * @return A {@link Movie} object containing all its detailed info.
	 * 
	 * @see Movie
	 * @see Movie#links
	 * @see it.jtomato.Links
	 * @see <a
	 *      href="http://developer.rottentomatoes.com/docs/read/json/v10/Movie_Info"
	 *      target="_blank"> Rotten Tomatoes API Documentation: Movie Info</a>
	 */
	public Movie getMovieAdditionalInfo(Movie movie) {
		Movie movieResult = null;
		URI uri = null;

		try {
			if (movie.id != null) {
				// Build URI from movie ID
				String path = urls.MOVIE_INFO + "/" + movie.id + ".json";
				HashMap<String, String> paramsMap = getParamsMap();
				uri = httpClient.buildURI(null, urls.ROTTENTOMATOES_API, path, paramsMap);
			} else if (movie.links.self != null) {
				// Otherwise build URI from movie self-link
				uri = new URI(movie.links.self);
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		if (uri != null) {
			// I have a URI let's submit the request
			String response = httpClient.get(uri);
			JsonParser parser = new JsonParser();
			JsonObject movieJson = parser.parse(response).getAsJsonObject();
			movieResult = parseMovieJson(movieJson);
		}

		return movieResult;
	}

	/**
	 * Returns similar movies for a given movie.
	 * 
	 * @param movie
	 *            A {@link Movie} object containing a valid movie id
	 * 
	 * @param country
	 *            Provides localized data for the selected country <a
	 *            href="http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2"
	 *            target="_blank"> (ISO 3166-1 alpha-2)</a> if available.
	 *            Otherwise, returns US data.
	 * 
	 * @param limit
	 *            Limits the number of similar movies returned. The maximum
	 *            value is 50, a request with a greater value is considered as
	 *            if it were issued with a limit equal to 50 . This parameter is
	 *            optional, it can be set to a negative value or zero value if
	 *            no limit is required.
	 * 
	 * @return A {@link Movie} object containing all its detailed info.
	 * 
	 * @see Movie
	 * @see <a
	 *      href="http://developer.rottentomatoes.com/docs/read/json/v10/Movie_Similar"
	 *      target="_blank"> Rotten Tomatoes API Documentation: Movie
	 *      Similar</a>
	 */
	public List<Movie> getSimilarMovies(Movie movie, String country, int limit) {
		List<Movie> movies = new ArrayList<Movie>();
		try {
			String path = urls.MOVIE_INFO + "/" + movie.id + "/similar.json";
			movies = getNonPaginatedResults(path, limit, country);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return movies;
	}

	public List<AbridgedCast> getMovieCast(String movieId) {
		List<AbridgedCast> result = new ArrayList<AbridgedCast>();
		String path = urls.MOVIE_INFO + "/" + movieId + "/cast.json";
		HashMap<String, String> paramsMap = getParamsMap();
		try {
			URI uri = httpClient.buildURI(null, urls.ROTTENTOMATOES_API, path, paramsMap);
			String response = httpClient.get(uri);
			JsonParser parser = new JsonParser();
			JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
			JsonArray castArray = jsonResponse.getAsJsonArray("cast");
			for (int i = 0; i < castArray.size(); i++) {
				JsonObject castJson = castArray.get(i).getAsJsonObject();
				AbridgedCast cast = gson.fromJson(castJson, AbridgedCast.class);
				result.add(cast);
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		return result;
	}

	public int getMovieReviews(String movieId, List<Review> reviews, String reviewType, int page, String country) {
		int total = 0;
		String path = urls.MOVIE_INFO + "/" + movieId + "/reviews.json";
		HashMap<String, String> paramsMap = getParamsMap();
		paramsMap.put("page", String.valueOf(page));
		try {
			URI uri = httpClient.buildURI(null, urls.ROTTENTOMATOES_API, path, paramsMap);
			String response = httpClient.get(uri);
			JsonParser parser = new JsonParser();
			JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
			JsonArray castArray = jsonResponse.getAsJsonArray("cast");
			for (int i = 0; i < castArray.size(); i++) {
				JsonObject castJson = castArray.get(i).getAsJsonObject();
				Review review = gson.fromJson(castJson, Review.class);
				reviews.add(review);
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return total;
	}

	private int getPaginatedResults(String path, List<Movie> result, int page, String country) throws URISyntaxException {
		HashMap<String, String> paramsMap = getParamsMap();
		if (country != null) {
			paramsMap.put("country", country);
		}
		paramsMap.put("page", String.valueOf(page));
		int total = 0;

		URI uri = httpClient.buildURI(null, urls.ROTTENTOMATOES_API, path, paramsMap);
		String response = httpClient.get(uri);

		JsonParser parser = new JsonParser();
		JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
		JsonArray movies = jsonResponse.get("movies").getAsJsonArray();

		total = jsonResponse.get("total").getAsInt();
		for (int i = 0; i < movies.size(); i++) {
			JsonObject movieJson = movies.get(i).getAsJsonObject();

			Movie movie = parseMovieJson(movieJson);
			if (movie != null) {
				result.add(movie);
			}
		}
		return total;
	}

	private List<Movie> getNonPaginatedResults(String path, int limit, String country) throws URISyntaxException {
		HashMap<String, String> paramsMap = getParamsMap();
		if (limit > 0) {
			if (limit > 50) {
				limit = 50;
			}
			paramsMap.put("limit", String.valueOf(limit));
		}
		if (country != null) {
			paramsMap.put("country", country);
		}
		List<Movie> result = new ArrayList<Movie>();
		URI uri = httpClient.buildURI(null, urls.ROTTENTOMATOES_API, path, paramsMap);
		String response = httpClient.get(uri);

		JsonParser parser = new JsonParser();
		JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
		JsonArray movies = jsonResponse.get("movies").getAsJsonArray();

		for (int i = 0; i < movies.size(); i++) {
			JsonObject movieJson = movies.get(i).getAsJsonObject();

			Movie movie = parseMovieJson(movieJson);
			if (movie != null) {
				result.add(movie);
			}
		}
		return result;
	}

	public Movie parseMovieJson(JsonObject movieJson) {
		Movie movie = gson.fromJson(movieJson, Movie.class);
		return movie;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public int getPage_limit() {
		return Integer.valueOf(page_limit);
	}

	public void setPage_limit(int page_limit) {
		this.page_limit = String.valueOf(page_limit);
	}
}
