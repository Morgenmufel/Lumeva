package lumeva.profileservice.domain.exceptions;

public class MissingHeaderException extends RuntimeException {
  public MissingHeaderException(String headerName) {
    super("Required HTTP header is missing or invalid: " + headerName);
  }
}
