package com.utt.foodcouriers_admin.data.common;

/**
 * Trả về kết quả từ server
 * @param <T>
 */
public class BaseResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final ErrorResponse error;

    private BaseResponse(boolean success, String message, T data, ErrorResponse error) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
    }

    public static <T> BaseResponse<T> success(T data, String message) {
        return new BaseResponse<>(true, message, data, null);
    }

    public static <T> BaseResponse<T> success(T data) {
        return success(data, "Success");
    }

    public static <T> BaseResponse<T> error(String code, String message) {
        return new BaseResponse<>(false, message, null, new ErrorResponse(code, message));
    }

    public static <T> BaseResponse<T> error(String message) {
        return error("UNKNOWN_ERROR", message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public ErrorResponse getError() {
        return error;
    }
}
