package ru.yandex.practicum.order.exception;

import feign.Response;
import feign.codec.ErrorDecoder;

public class CommerceFeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() >= 500) {
            return new OrderProcessingException("Невозможно оформить заказ, попробуйте позднее");
        }

        return defaultDecoder.decode(methodKey, response);
    }
}
