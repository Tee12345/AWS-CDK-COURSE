package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.*;

public class ECommerceEcsCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        Environment environment = Environment.builder()
                .account("767397896801")
                .region("eu-north-1")
                .build();

        Map<String, String> infraTags = new HashMap<>();
        infraTags.put("team", "SiecolaCode");
        infraTags.put("cost", "EcommerceInfra");

        EcrStack ecrStack =  new EcrStack(app, "Ecr", StackProps.builder()
                .env(environment)
                .tags(infraTags)
                .build());

        VpcStack vpcStack = new VpcStack(app, "Vpc", StackProps.builder()
                .env(environment)
                .tags(infraTags)
                .build());

        ClusterStack clusterStack = new ClusterStack(app, "Cluster", StackProps.builder()
                .env(environment)
                .tags(infraTags)
                .build(), new ClusterStackProps(vpcStack.getVpc()));

                clusterStack.addDependency(vpcStack);

        NlbStack nlbStack = new NlbStack(app, "Nlb", StackProps.builder()
                        .env(environment)
                        .tags(infraTags)
                        .build(), new NlbStackProps(vpcStack.getVpc()));

                nlbStack.addDependency(vpcStack);

        Map<String, String> productsServiceTags = new HashMap<>();
        productsServiceTags.put("team", "SiecolaCode");
        productsServiceTags.put("cost", "ProductsService");

         ProductsServiceStack productServiceStack = new ProductsServiceStack(app, "ProductsService",
                 StackProps.builder()
                         .env(environment)
                         .tags(productsServiceTags)
                         .build(),
                 new ProductsServiceProps(
                         vpcStack.getVpc(),
                         clusterStack.getCluster(),
                         nlbStack.getNetworkLoadBalancer(),
                         nlbStack.getApplicationLoadBalancer(),
                         ecrStack.getProductsServiceRepository()));
         productServiceStack.addDependency(vpcStack);
        productServiceStack.addDependency(clusterStack);
        productServiceStack.addDependency(nlbStack);
        productServiceStack.addDependency(ecrStack);

        app.synth();
    }
}

