package lol.duckyyy.client.api;

public class ApiResponse<T> {
    public T data = null;
    public int v;
    public ApiError error = null;
}
