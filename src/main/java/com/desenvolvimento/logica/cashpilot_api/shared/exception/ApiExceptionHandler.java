package com.desenvolvimento.logica.cashpilot_api.shared.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<FieldViolation> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldViolation(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        ProblemDetail problem = validationProblem(errors);

        return handleExceptionInternal(
                exception, problem, headers, status, request
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleServiceValidation(
            ConstraintViolationException exception) {

        List<FieldViolation> errors = exception.getConstraintViolations()
                .stream()
                .map(violation -> new FieldViolation(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();

        return validationProblem(errors);
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ProblemDetail handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException exception) {

        return emailConflict();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(
            DataIntegrityViolationException exception) {

        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof
                    org.hibernate.exception.ConstraintViolationException violation
                    && "uk_users_email".equals(violation.getConstraintName())) {

                return emailConflict();
            }

            cause = cause.getCause();
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Não foi possível concluir a operação."
        );
        problem.setTitle("Erro ao persistir os dados");

        return problem;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(
            UserNotFoundException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Usuário não encontrado."
        );
        problem.setTitle("Recurso não encontrado");

        return problem;
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ProblemDetail handleForbiddenOperation(
            ForbiddenOperationException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Você não tem permissão para realizar esta operação."
        );
        problem.setTitle("Acesso negado");

        return problem;
    }

    @ExceptionHandler(InvalidEmailVerificationTokenException.class)
    public ProblemDetail handleInvalidEmailVerificationToken(
            InvalidEmailVerificationTokenException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "O link de confirmação é inválido ou expirou."
        );
        problem.setTitle("Falha na confirmação de e-mail");

        return problem;
    }

    private ProblemDetail validationProblem(List<FieldViolation> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Confira os campos informados."
        );
        problem.setTitle("Dados inválidos");
        problem.setProperty("errors", errors);

        return problem;
    }

    private ProblemDetail emailConflict() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Não foi possível cadastrar com o e-mail informado."
        );
        problem.setTitle("Conflito no cadastro");

        return problem;
    }

    public record FieldViolation(String field, String message) {
    }
}
