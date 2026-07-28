package site.benepay.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonSyntaxException;

import site.benepay.service.ServiceException;
import site.benepay.util.JsonUtil;

public abstract class BaseApiServlet extends HttpServlet {
	protected <T> T readJson(HttpServletRequest request, Class<T> type) {
		try {
			StringBuilder body = new StringBuilder();
			try (BufferedReader reader = request.getReader()) {
				String line;
				while ((line = reader.readLine()) != null)
					body.append(line);
			}
			if (body.length() == 0)
				throw new ServiceException(400, "EMPTY_BODY", "요청 본문이 필요합니다.");
			return JsonUtil.gson().fromJson(body.toString(), type);
		} catch (JsonSyntaxException | IOException e) {
			throw new ServiceException(400, "INVALID_JSON", "JSON 요청 형식이 올바르지 않습니다.", e);
		}
	}

	protected void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
		response.setStatus(status);
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write(JsonUtil.gson().toJson(body));
	}

	protected void writeSuccess(HttpServletResponse response, Object data) throws IOException {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("success", true);
		result.put("data", data);
		writeJson(response, 200, result);
	}

	protected void handleError(HttpServletResponse response, Exception exception) throws IOException {
		int status = 500;
		String code = "INTERNAL_SERVER_ERROR";
		String message = "서버 처리 중 오류가 발생했습니다.";
		if (exception instanceof ServiceException) {
			ServiceException serviceException = (ServiceException)exception;
			status = serviceException.getStatusCode();
			code = serviceException.getErrorCode();
			message = serviceException.getMessage();
		} else {
			exception.printStackTrace();
		}
		Map<String, Object> error = new LinkedHashMap<String, Object>();
		error.put("success", false);
		error.put("errorCode", code);
		error.put("message", message);
		writeJson(response, status, error);
	}

	protected long parseLong(String value, String fieldName) {
		try {
			return Long.parseLong(value);
		} catch (Exception e) {
			throw new ServiceException(400, "INVALID_PARAMETER", fieldName + " 값이 올바르지 않습니다.");
		}
	}
}
