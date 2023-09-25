package side.side.exception.data;

public class ApiFailureException extends RuntimeException{

    private static final long serialVersionUID = -4767072888291009622L;

    public ApiFailureException() {
        super("뉴스 정보를 불러오는데 문제가 발생했습니다.");
    }

    public ApiFailureException(String message) {
        super(message);
    }
}
