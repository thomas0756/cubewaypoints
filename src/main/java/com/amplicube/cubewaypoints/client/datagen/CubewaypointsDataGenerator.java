package com.amplicube.cubewaypoints.client.datagen;

import com.amplicube.cubewaypoints.client.datagen.lang.en_au;
import com.amplicube.cubewaypoints.client.datagen.lang.en_ca;
import com.amplicube.cubewaypoints.client.datagen.lang.en_gb;
import com.amplicube.cubewaypoints.client.datagen.lang.en_us;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class CubewaypointsDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

        pack.addProvider(en_us::new);
        pack.addProvider(en_au::new);
        pack.addProvider(en_ca::new);
        pack.addProvider(en_gb::new);
    }
}