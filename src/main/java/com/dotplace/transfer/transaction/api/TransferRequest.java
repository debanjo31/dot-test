package com.dotplace.transfer.transaction.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record TransferRequest(
    @NotBlank @Size(max = 34) String sourceAccountNumber,
    @NotBlank @Size(max = 34) String destinationAccountNumber,
    @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
    @Size(max = 255) String description) {}
