package com.socotra.deployment.customer.personalauto;

import com.socotra.coremodel.ValidationItem;
import com.socotra.deployment.customer.*;

public class MyValidationPlugin implements ValidationPlugin {

    @Override
    public ValidationItem validate(PersonalAutoQuoteRequest request) {
        PersonalAutoQuote personalAutoQuote = request.quote();
        ValidationItem.Builder builder = ValidationItem.builder();

        for (PersonalVehicle vehicle : personalAutoQuote.personalVehicles()) {
            // Bodily Injury + Collision Deductible Match
            // if (vehicle.bodilyInjury() != null && vehicle.collision() != null &&
            //     vehicle.bodilyInjury().PADeductible() != vehicle.collision().PADeductible()) {
            //     builder.addError("When both the bodilyInjury and collision coverages are selected, " +
            //         "their deductibles must match. bodilyInjury.PADeductible is %s, while collision.PADeductible is %s"
            //         .formatted(vehicle.bodilyInjury().PADeductible(), vehicle.collision().PADeductible()));
            // }

            // Fire + Theft Deductible Match
            if (vehicle.fire() != null && vehicle.theft() != null &&
                vehicle.fire().PADeductible() != vehicle.theft().PADeductible()) {
                builder.addError("When you have opted for both Fire and Theft Coverages, your Deductibles must match. " +
                    "fire.PADeductible is %s, while theft.PADeductible is %s"
                    .formatted(vehicle.fire().PADeductible(), vehicle.theft().PADeductible()));
            }

            // Baby Seat requires Own Damage
            if (vehicle.babySeat() != null && vehicle.ownDamage() == null) {
                builder.addError("Baby Seat coverage applies only when Own Damage Coverage is selected.");
            }
        }

        return builder.build();
    }

    @Override
    public ValidationItem validate(PersonalAutoRequest request) {
        if (request.segment().isEmpty()) {
            return ValidationItem.builder().build();
        }

        var segment = request.segment().get();
        ValidationItem.Builder builder = ValidationItem.builder();

        for (PersonalVehicle vehicle : segment.personalVehicles()) {
            // Bodily Injury + Collision Deductible Match
            // if (vehicle.bodilyInjury() != null && vehicle.collision() != null &&
            //     vehicle.bodilyInjury().PADeductible() != vehicle.collision().PADeductible()) {
            //     builder.addError("When both the bodilyInjury and collision coverages are selected, " +
            //         "their deductibles must match. bodilyInjury.PADeductible is %s, while collision.PADeductible is %s"
            //         .formatted(vehicle.bodilyInjury().PADeductible(), vehicle.collision().PADeductible()));
            // }

            // Fire + Theft Deductible Match
            if (vehicle.fire() != null && vehicle.theft() != null &&
                vehicle.fire().PADeductible() != vehicle.theft().PADeductible()) {
                builder.addError("When you have opted for both Fire and Theft Coverages, your Deductibles must match. " +
                    "fire.PADeductible is %s, while theft.PADeductible is %s"
                    .formatted(vehicle.fire().PADeductible(), vehicle.theft().PADeductible()));
            }

            // Baby Seat requires Own Damage
            if (vehicle.babySeat() != null && vehicle.ownDamage() == null) {
                builder.addError("Baby Seat coverage applies only when Own Damage Coverage is selected.");
            }
        }

        return builder.build();
    }
}