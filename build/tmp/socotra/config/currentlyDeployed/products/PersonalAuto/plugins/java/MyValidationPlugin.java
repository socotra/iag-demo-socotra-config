package com.socotra.deployment.customer.personalauto;
import com.socotra.deployment.customer.*;

import com.socotra.coremodel.ValidationItem;
import com.socotra.deployment.customer.ValidationPlugin;
import com.socotra.deployment.customer.PersonalAutoQuote;

public class MyValidationPlugin implements ValidationPlugin {
    @Override
    public ValidationItem validate(PersonalAutoQuoteRequest request){
        PersonalAutoQuote personalAutoQuote = request.quote();

        for(PersonalVehicle vehicle : personalAutoQuote.personalVehicles())
            if(vehicle.bodilyInjury() != null && vehicle.collision() != null) {
                if(vehicle.bodilyInjury().PADeductible() != vehicle.collision().PADeductible()) {
                    String errorStr = "When both the bodilyInjury and collision coverages are selected, " +
                        "their deductibles must match. bodilyInjury.PADeductible is %s, while collision.PADeductible is %s"
                            .formatted(vehicle.bodilyInjury().PADeductible(), vehicle.collision().PADeductible());
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

        for(PersonalVehicle vehicle : segment.personalVehicles())
            if(vehicle.bodilyInjury() != null && vehicle.collision() != null) {
                if(vehicle.bodilyInjury().PADeductible() != vehicle.collision().PADeductible()) {
                    String errorStr = "When both the bodilyInjury and collision coverages are selected, " +
                        "their deductibles must match. bodilyInjury.PADeductible is %s, while collision.PADeductible is %s"
                            .formatted(vehicle.bodilyInjury().PADeductible(), vehicle.collision().PADeductible());
                    return ValidationItem.builder()
                        .addError(errorStr)
                        .build();
                }
            }
        return ValidationItem.builder().build();
    }
}
