package org.refactoringminer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

// import com.sun.net.httpserver.Headers;
// import com.sun.net.httpserver.HttpExchange;
// import com.sun.net.httpserver.HttpHandler;
// import com.sun.net.httpserver.HttpServer;
import static spark.Spark.*;

public class RefactoringMinerHttpSparkServer {

	public static void main(String[] args) throws Exception {
		Properties prop = new Properties();
		InputStream input = new FileInputStream("server.properties");
		prop.load(input);
		// String hostName = prop.getProperty("hostname");
		int port = Integer.parseInt(prop.getProperty("port"));
		
		port(port);
		GitService gitService = new GitServiceImpl();
		List<Refactoring> detectedRefactorings = new ArrayList<Refactoring>();

		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		get("/RefactoringMiner", (req, res) -> {
			System.out.println("=========RefactoringMiner=========");
			res.header("Content-Type", "application/json");
			// res.header("Access-Control-Allow-Origin", "http://127.0.0.1:8080"); // TODO accept more than one origin
			res.header("Access-Control-Allow-Origin", "*"); // TODO accept more than one origin
			res.header("Access-Control-Allow-Credentials", "true");
			Set<String> params = req.queryParams();
			String gitURL = req.queryParams("gitURL");
			int timeout = Integer.parseInt(req.queryParams("timeout"));
			if (params.contains("commitId")) {
				String commitId = req.queryParams("commitId");
					
				miner.detectAtCommit(gitURL, commitId, new RefactoringHandler() {
					@Override
					public void handle(String commitId, List<Refactoring> refactorings) {
						detectedRefactorings.addAll(refactorings);
					}
				}, timeout);

				String response = JSON(gitURL, commitId, detectedRefactorings);
				System.out.println(response);
				return response;
			} else if (params.contains("start") && params.contains("end")) {
				String start = req.queryParams("start");
				String end = req.queryParams("end");
				Repository repo = gitService.cloneIfNotExists(
					"/tmp/refactoring-toy-example",
					gitURL);
					
				miner.detectBetweenCommits(repo, start, end, new RefactoringHandler() {
					@Override
					public void handle(String commitId, List<Refactoring> refactorings) {
						detectedRefactorings.addAll(refactorings);
					}
				});

				String response = JSON(gitURL, end, detectedRefactorings);
				System.out.println(response);
				return response;
			} else if (params.contains("startTag") && params.contains("endTag")) {
				String start = req.queryParams("startTag");
				String end = req.queryParams("endTag");
				Repository repo = gitService.cloneIfNotExists(
					"/tmp/refactoring-toy-example",
					gitURL);
					
				miner.detectBetweenTags(repo, start, end, new RefactoringHandler() {
					@Override
					public void handle(String commitId, List<Refactoring> refactorings) {
						detectedRefactorings.addAll(refactorings);
					}
				});

				String response = JSON(gitURL, end, detectedRefactorings);
				System.out.println(response);
				return response;
			} else if (params.contains("old") && params.contains("new")) {
				String old = req.queryParams("old");
				String neww = req.queryParams("new");
				Repository repo = gitService.cloneIfNotExists(
					"/tmp/refactoring-toy-example",
					gitURL);
					
				miner.detectBetweenCommits(repo, old, neww, new RefactoringHandler() {
					@Override
					public void handle(String commitId, List<Refactoring> refactorings) {
						detectedRefactorings.addAll(refactorings);
					}
				});

				String response = JSON(gitURL, neww, detectedRefactorings);
				System.out.println(response);
				return response;
			} else {
				Repository repo = gitService.cloneIfNotExists(
					"/tmp/refactoring-toy-example",
					gitURL);
				miner.detectAll(repo, "master", new RefactoringHandler() {
					@Override
					public void handle(String commitId, List<Refactoring> refactorings) {
						detectedRefactorings.addAll(refactorings);
					}
				});

				String response = JSON(gitURL, "aaa", detectedRefactorings);
				System.out.println(response);
				return response;
			}

			// URI requestURI = exchange.getRequestURI();
			// String query = requestURI.getQuery();
			// Map<String, String> queryToMap = queryToMap(query);
			// System.out.println(query);

			// String gitURL = queryToMap.get("gitURL");
			// String commitId = queryToMap.get("commitId");
			// int timeout = Integer.parseInt(queryToMap.get("timeout"));
			// exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			// exchange.sendResponseHeaders(200, response.length());
			// OutputStream os = exchange.getResponseBody();
			// os.write(response.getBytes());
			// os.close();

		});

		// InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getByName(hostName), port);
		// HttpServer server = HttpServer.create(inetSocketAddress, 0);
		// server.createContext("/RefactoringMiner", new MyHandler());
		// server.setExecutor(new ThreadPoolExecutor(4, 8, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100)));
		// server.start();
		// System.out.println(InetAddress.getLocalHost());
	}

	// static class MyHandler implements HttpHandler {
	// 	@Override
	// 	public void handle(HttpExchange exchange) throws IOException {
	// 		printRequestInfo(exchange);
	// 		URI requestURI = exchange.getRequestURI();
	// 		String query = requestURI.getQuery();
	// 		Map<String, String> queryToMap = queryToMap(query);
	// 		System.out.println(query);

	// 		String gitURL = queryToMap.get("gitURL");
	// 		String commitId = queryToMap.get("commitId");
	// 		int timeout = Integer.parseInt(queryToMap.get("timeout"));
	// 		List<Refactoring> detectedRefactorings = new ArrayList<Refactoring>();

	// 		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
	// 		miner.detectAtCommit(gitURL, commitId, new RefactoringHandler() {
	// 			@Override
	// 			public void handle(String commitId, List<Refactoring> refactorings) {
	// 				detectedRefactorings.addAll(refactorings);
	// 			}
	// 		}, timeout);

	// 		String response = JSON(gitURL, commitId, detectedRefactorings);
	// 		System.out.println(response);
	// 		exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
	// 		exchange.sendResponseHeaders(200, response.length());
	// 		OutputStream os = exchange.getResponseBody();
	// 		os.write(response.getBytes());
	// 		os.close();
	// 	}
	// }

	private static Map<String, String> queryToMap(String query) {
		Map<String, String> result = new HashMap<>();
		for (String param : query.split("&")) {
			String[] entry = param.split("=");
			if (entry.length > 1) {
				result.put(entry[0], entry[1]);
			}
			else {
				result.put(entry[0], "");
			}
		}
		return result;
	}

	private static String JSON(String gitURL, String currentCommitId, List<Refactoring> refactoringsAtRevision) {
		StringBuilder sb = new StringBuilder();
		sb.append("{").append("\n");
		sb.append("\"").append("commits").append("\"").append(": ");
		sb.append("[");
		sb.append("{");
		sb.append("\t").append("\"").append("repository").append("\"").append(": ").append("\"").append(gitURL).append("\"").append(",").append("\n");
		sb.append("\t").append("\"").append("sha1").append("\"").append(": ").append("\"").append(currentCommitId).append("\"").append(",").append("\n");
		String url = "https://github.com/" + gitURL.substring(19, gitURL.indexOf(".git")) + "/commit/" + currentCommitId;
		sb.append("\t").append("\"").append("url").append("\"").append(": ").append("\"").append(url).append("\"").append(",").append("\n");
		sb.append("\t").append("\"").append("refactorings").append("\"").append(": ");
		sb.append("[");
		int counter = 0;
		for(Refactoring refactoring : refactoringsAtRevision) {
			sb.append(refactoring.toJSON());
			if(counter < refactoringsAtRevision.size()-1) {
				sb.append(",");
			}
			sb.append("\n");
			counter++;
		}
		sb.append("]");
		sb.append("}");
		sb.append("]").append("\n");
		sb.append("}");
		return sb.toString();
	}

// 	private static void printRequestInfo(HttpExchange exchange) {
// 		System.out.println("-- headers --");
// 		Headers requestHeaders = exchange.getRequestHeaders();
// 		requestHeaders.entrySet().forEach(System.out::println);

// 		System.out.println("-- HTTP method --");
// 		String requestMethod = exchange.getRequestMethod();
// 		System.out.println(requestMethod);

// 		System.out.println("-- query --");
// 		URI requestURI = exchange.getRequestURI();
// 		String query = requestURI.getQuery();
// 		System.out.println(query);
// 	}
}
