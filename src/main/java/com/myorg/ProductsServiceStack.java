package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.Stack;
//import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecr.*;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.logs.*;
//import software.amazon.awscdk.services.servicecatalog.*;
import software.constructs.*;

import java.util.*;

public class ProductsServiceStack extends Stack {

    public ProductsServiceStack(final Construct scope, final String id, final StackProps props,
                                ProductsServiceProps productsServiceProps) {
        super(scope, id, props);

        FargateTaskDefinition fargateTaskDefinition = new FargateTaskDefinition(this, "TaskDefinition",
                FargateTaskDefinitionProps.builder()
                        .family("products-service")
                        .cpu(512)
                        .memoryLimitMiB(1024)
                        .build());

        AwsLogDriver logDriver = new AwsLogDriver(AwsLogDriverProps.builder()
                .logGroup(new LogGroup(this, "LogGroup",
                        LogGroupProps.builder()
                                .logGroupName("ProductsService")
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .retention(RetentionDays.ONE_MONTH)
                        .build()))
                .streamPrefix("ProductsService")
                .build());

    Map<String, String> envVariables = new HashMap<>();
    envVariables.put("SERVER_PORT", "8080");

        fargateTaskDefinition.addContainer("ProductsServiceContainer",
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromEcrRepository(productsServiceProps.repository(), "1.0.0"))
                        .containerName("productsservice")
                        .logging(logDriver)
                        .portMappings(Collections.singletonList(PortMapping.builder()
                                        .containerPort(8080)
                                        .protocol(Protocol.TCP)
                                .build()))
                        .environment(envVariables)
                        .build());
        ApplicationListener applicationListener = productsServiceProps.applicationLoadBalancer()
                .addListener("ProductsServiceAlbListener", ApplicationListenerProps.builder()
                        .port(8080)
                        .protocol(ApplicationProtocol.HTTP)
                        .loadBalancer(productsServiceProps.applicationLoadBalancer())
                        .build());

        FargateService fargateService = new FargateService(this, "", FargateServiceProps.builder()
                .serviceName("ProductsService")
                .cluster(productsServiceProps.cluster())
                .taskDefinition(fargateTaskDefinition)
                .desiredCount(2)
                //Do not do the below in Production!!!
                //.assignPublicIp(true)
                .assignPublicIp(false)
                .build());

        productsServiceProps.repository().grantPull(Objects.requireNonNull(fargateTaskDefinition.getExecutionRole()));
        fargateService.getConnections().getSecurityGroups().get(0).addIngressRule(Peer.anyIpv4(), Port.tcp(8080));

        applicationListener.addTargets("ProductsServiceAlbTarget",
               AddApplicationTargetsProps.builder()
                       .targetGroupName("productsServiceAlb")
                       .port(8080)
                       .protocol(ApplicationProtocol.HTTP)
                       .targets(Collections.singletonList(fargateService))
                       .deregistrationDelay(Duration.seconds(30))
                       .healthCheck(HealthCheck.builder()
                               .enabled(true)
                               .interval(Duration.seconds(30))
                               .timeout(Duration.seconds(10))
                               .path("/actuator/health")
                               .port("8080")
                               .build())
                       .build());

        NetworkListener networkListener = productsServiceProps.networkLoadBalancer()
                .addListener("ProductsServiceNlbListener", BaseNetworkListenerProps.builder()
                        .port(8080)
                        .protocol(
                                software.amazon.awscdk.services.elasticloadbalancingv2.Protocol.TCP
                        )
                        .build());
        networkListener.addTargets("ProductsServiceNlbTarget",
                AddNetworkTargetsProps.builder()
                        .port(8080)
                        .protocol(software.amazon.awscdk.services.elasticloadbalancingv2.Protocol.TCP)
                        .targetGroupName("productsServiceNlb")
                        .targets(Collections.singletonList(
                                fargateService.loadBalancerTarget(LoadBalancerTargetOptions.builder()
                                                .containerName("productsService")
                                                .containerPort(8080)
                                                .protocol(Protocol.TCP)
                                        .build())
                        ))
                        .build());
        }
    }

  record ProductsServiceProps(
          Vpc vpc,
          Cluster cluster,
          NetworkLoadBalancer networkLoadBalancer,
          ApplicationLoadBalancer applicationLoadBalancer,
          Repository repository
  ) {}


