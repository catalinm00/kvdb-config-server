package com.kvdbcs.infrastructure.controller;

import com.kvdbcs.infrastructure.controller.request.CreateDbInstanceRequest;
import com.kvdbcs.domain.model.DbInstance;
import com.kvdbcs.application.service.CreateDbInstanceService;
import com.kvdbcs.application.service.FindAllInstancesService;
import com.kvdbcs.application.service.command.CreateDbInstanceCommand;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

import java.util.List;

@Controller("/api/v1") // Mejor forma: sin slash al final
@OpenAPIDefinition(
        info = @Info(title = "DbInstance API", version = "1.0")
)
public class InstanceController {

    private final CreateDbInstanceService createDbInstanceService;
    private final FindAllInstancesService findAllInstancesService;

    public InstanceController(CreateDbInstanceService createDbInstanceService,
                              FindAllInstancesService findAllInstancesService) {
        this.createDbInstanceService = createDbInstanceService;
        this.findAllInstancesService = findAllInstancesService;
    }

    @Post(uri = "/instances", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public HttpResponse<DbInstance> createInstance(@Body CreateDbInstanceRequest request) {
        try {
            DbInstance dbInstance = createDbInstanceService.execute(
                    new CreateDbInstanceCommand(request.host(), request.port())
            );
            return HttpResponse.created(dbInstance);
        } catch (Exception e) {
            return HttpResponse.serverError();
        }
    }

    @Get(uri = "/instances", produces = MediaType.APPLICATION_JSON)
    public HttpResponse<List<DbInstance>> getAllInstances() {
        List<DbInstance> instances = findAllInstancesService.execute();
        return HttpResponse.ok(instances);
    }
}
