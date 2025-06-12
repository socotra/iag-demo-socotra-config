package com.socotra.deployment.customer.personalauto;
import com.socotra.deployment.customer.*;

import com.socotra.coremodel.ValidationItem;
import com.socotra.deployment.customer.ValidationPlugin;
import com.socotra.deployment.customer.PersonalAutoQuote;

public class MyValidationPlugin implements ValidationPlugin {
    @Override
    public ValidationItem validate(PersonalAutoQuoteRequest request){
        PersonalAutoQuote personalAutoQuote = request.quote();

        for(PersonalVehicle vehicle : personalAutoQuote.personalVehicles()) {
            // Fire + Theft Deductible Match
            if (vehicle.fire() != null && vehicle.theft() != null) {
                if (vehicle.fire().PADeductible() != vehicle.theft().PADeductible()) {
                    String errorStr = "When you have opted for both Fire and Theft Coverages, your Deductibles must match. " +
                            "fire.PADeductible is %s, while theft.PADeductible is %s"
                                    .formatted(vehicle.fire().PADeductible(), vehicle.theft().PADeductible());
                    return ValidationItem.builder()
                            .elementType(personalAutoQuote.type())
                            .addError(errorStr)
                            .build();
                }
            }
            // Baby Seat requires Own Damage
            if (vehicle.babySeat() != null && vehicle.ownDamage() == null) {
                String errorStr = "Baby Seat coverage applies only when Own Damage Coverage is selected.";
                return ValidationItem.builder()
                        .elementType(personalAutoQuote.type())
                        .addError(errorStr)
                        .build();
            }
        }
        return ValidationItem.builder().build();
    }

    public ValidationItem validate(PersonalAutoRequest request) {
        if (request.segment().isEmpty()) {
            return ValidationItem.builder().build();
        }

        var segment = request.segment().get();

        for(PersonalVehicle vehicle : segment.personalVehicles()) {
            // Fire + Theft Deductible Match
            if (vehicle.fire() != null && vehicle.theft() != null) {
                if (vehicle.fire().PADeductible() != vehicle.theft().PADeductible()) {
                    String errorStr = "When you have opted for both Fire and Theft Coverages, your Deductibles must match. " +
                            "fire.PADeductible is %s, while theft.PADeductible is %s"
                                    .formatted(vehicle.fire().PADeductible(), vehicle.theft().PADeductible());
                    return ValidationItem.builder()
                            .addError(errorStr)
                            .build();
                }
            }
            // Baby Seat requires Own Damage
            if (vehicle.babySeat() != null && vehicle.ownDamage() == null) {
                String errorStr = "Baby Seat coverage applies only when Own Damage Coverage is selected.";
                return ValidationItem.builder()
                        .addError(errorStr)
                        .build();
            }
        }
        return ValidationItem.builder().build();
    }
}
