package cz.jirikfi.monitoringsystembackend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.validator.routines.InetAddressValidator;

public class IpAddressValidator implements ConstraintValidator<IpAddress, String> {

    private final InetAddressValidator validator = InetAddressValidator.getInstance();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        return validator.isValidInet4Address(value) || validator.isValidInet6Address(value);
    }
}
