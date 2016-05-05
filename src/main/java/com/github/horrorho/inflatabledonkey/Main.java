/* 
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.horrorho.inflatabledonkey;

import com.github.horrorho.inflatabledonkey.args.Property;
import com.github.horrorho.inflatabledonkey.args.PropertyLoader;
import com.github.horrorho.inflatabledonkey.chunk.engine.standard.StandardChunkEngine;
import com.github.horrorho.inflatabledonkey.chunk.store.disk.DiskChunkStore;
import com.github.horrorho.inflatabledonkey.cloud.AssetDownloader;
import com.github.horrorho.inflatabledonkey.cloud.AuthorizeAssets;
import com.github.horrorho.inflatabledonkey.cloud.accounts.Account;
import com.github.horrorho.inflatabledonkey.cloud.accounts.Accounts;
import com.github.horrorho.inflatabledonkey.cloud.auth.Auth;
import com.github.horrorho.inflatabledonkey.cloud.auth.Authenticator;
import com.github.horrorho.inflatabledonkey.data.backup.Asset;
import com.github.horrorho.inflatabledonkey.data.backup.Assets;
import com.github.horrorho.inflatabledonkey.data.backup.BackupAccount;
import com.github.horrorho.inflatabledonkey.data.backup.Device;
import com.github.horrorho.inflatabledonkey.data.backup.Snapshot;
import com.github.horrorho.inflatabledonkey.data.backup.SnapshotID;
import com.github.horrorho.inflatabledonkey.pcs.xfile.FileAssembler;
import com.github.horrorho.inflatabledonkey.protocol.CloudKit;
import com.github.horrorho.inflatabledonkey.util.FileUtils;
import com.github.horrorho.inflatabledonkey.util.ListUtils;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * InflatableDonkey.
 *
 * @author Ahseya
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * @param args the command line arguments
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        try {
            if (!PropertyLoader.instance().test(args)) {
                return;
            }
        } catch (IllegalArgumentException ex) {
            System.out.println("Argument error: " + ex.getMessage());
            System.out.println("Try '" + Property.APP_NAME.value() + " --help' for more information.");
            System.exit(-1);
        }

        // SystemDefault HttpClient.
        // TODO concurrent
        CloseableHttpClient httpClient = HttpClients.custom()
                .setUserAgent("CloudKit/479 (13A404)")
                .useSystemProperties()
                .build();

        // Auth
        // TODO rework when we have UncheckedIOException for Authenticator
        Auth auth = Property.AUTHENTICATION_TOKEN.value()
                .map(Auth::new)
                .orElse(null);

        if (auth == null) {
            auth = Authenticator.authenticate(
                    httpClient,
                    Property.AUTHENTICATION_APPLEID.value().get(),
                    Property.AUTHENTICATION_PASSWORD.value().get());
        }
        logger.debug("-- main() - auth: {}", auth);
        logger.info("-- main() - dsPrsID:mmeAuthToken: {}:{}", auth.dsPrsID(), auth.mmeAuthToken());

        if (Property.ARGS_TOKEN.booleanValue().orElse(false)) {
            System.out.println("DsPrsID:mmeAuthToken " + auth.dsPrsID() + ":" + auth.mmeAuthToken());
            return;
        }

        logger.info("-- main() - Apple ID: {}", Property.AUTHENTICATION_APPLEID.value());
        logger.info("-- main() - password: {}", Property.AUTHENTICATION_PASSWORD.value());
        logger.info("-- main() - token: {}", Property.AUTHENTICATION_TOKEN.value());

        // Account
        Account account = Accounts.account(httpClient, auth);

        // Backup
        Backup backup = Backup.create(httpClient, account);

        // BackupAccount
        BackupAccount backupAccount = backup.backupAccount(httpClient);
        logger.debug("-- main() - backup account: {}", backupAccount);

        // Devices
        List<Device> devices = backup.devices(httpClient, backupAccount.devices());
        logger.debug("-- main() - device count: {}", devices.size());

        // Snapshots
        List<SnapshotID> snapshotIDs = devices.stream()
                .map(Device::snapshots)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        logger.info("-- main() - total snapshot count: {}", snapshotIDs.size());

        Map<String, Snapshot> snapshots = backup.snapshot(httpClient, snapshotIDs)
                .stream()
                .collect(Collectors.toMap(
                        s -> s.record()
                        .getRecordIdentifier()
                        .getValue()
                        .getName(),
                        Function.identity()));

        // Selection
        int deviceIndex = Property.SELECT_DEVICE_INDEX.intValue().get();
        Optional<String> deviceSerialNumber = Property.SELECT_DEVICE_SERIALNUMBER.value();
        int snapshotIndex = Property.SELECT_SNAPSHOT_INDEX.intValue().get();
        logger.info("-- main() - arg device index: {}", deviceIndex);
        logger.info("-- main() - arg device serial number: {}", deviceSerialNumber);
        logger.info("-- main() - arg snapshot index: {}", snapshotIndex);

        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            List<SnapshotID> deviceSnapshotIDs = device.snapshots();

            System.out.println(i + " " + device.info());

            for (int j = 0; j < deviceSnapshotIDs.size(); j++) {
                SnapshotID sid = deviceSnapshotIDs.get(j);
                System.out.println("\t" + j + snapshots.get(sid.id()).info() + "   " + sid.timestamp());
            }
        }
        if (Property.PRINT_SNAPSHOTS.booleanValue().orElse(false)) {
            return;
        }

        Device device = null;

        if (deviceSerialNumber.isPresent()) {
            device = devices.stream().filter(d -> d.serialNumber().toLowerCase() == deviceSerialNumber.get().toLowerCase()).findFirst().orElse(null);

            if (device == null) {
                System.out.println("No such device: " + deviceSerialNumber);
            }

            System.out.println("Selected device: " + deviceSerialNumber.get() + ", " + device.info());
        }
        else {
            if (deviceIndex >= devices.size()) {
                System.out.println("No such device: " + deviceIndex);
            }

            device = devices.get(deviceIndex);

            System.out.println("Selected device: " + deviceIndex + ", " + device.info());
        }

        if (snapshotIndex >= device.snapshots().size()) {
            System.out.println("No such snapshot for selected device: " + snapshotIndex);
        }

        String selected = device.snapshots().get(snapshotIndex).id();
        Snapshot snapshot = snapshots.get(selected);

        System.out.println("Selected snapshot: " + snapshotIndex + ", " + snapshot.info());

        // Asset list.
        List<Assets> assetsList = backup.assetsList(httpClient, snapshot);
        logger.info("-- main() - assets count: {}", assetsList.size());

        // Output domains --domains option
        if (Property.PRINT_DOMAIN_LIST.booleanValue().orElse(false)) {
            System.out.println("Domains / file count:");
            assetsList.stream()
                    .filter(a -> a.domain().isPresent())
                    .map(a -> a.domain().get() + " / " + a.files().size())
                    .sorted()
                    .forEach(System.out::println);
            return;
            // TODO check Assets without domain information.
        }

        // Domains + file extensions filter --filter option
        String filterPath = Property.FILTER.value().orElse("");
        Predicate<Optional<String>> domainFilter = null;
        Predicate<Asset> assetFilter = null;

        if (!filterPath.isEmpty()) {
            JSONParser parser = new JSONParser();
            try {
                JSONArray jsonArray = (JSONArray) parser.parse(new FileReader(filterPath));

                List<Filter> filters = new ArrayList<Filter>();

                for (Object obj : jsonArray)
                {
                    JSONObject jsonObject = (JSONObject) obj;

                    String domain = (String) jsonObject.get("domain");
                    String path = (String) jsonObject.get("path");

                    Filter filter = new Filter(domain.toLowerCase(Locale.US), path.toLowerCase(Locale.US));
                    filters.add(filter);
                }

                domainFilter = domain ->
                {
                    boolean found = false;

                    for (Filter filter : filters) {
                        found = domain
                                .map(d -> d.toLowerCase(Locale.US))
                                .map(d -> d.matches(filter.getDomain()))
                                .orElse(false);

                        if (found)
                        {
                            break; // for
                        }
                    }

                    return found;
                };

                assetFilter = asset -> asset
                    .relativePath()
                    .map(path ->
                    {
                        boolean found = false;

                        for (Filter filter : filters) {
                            found = asset.domain()
                                    .map(d -> d.toLowerCase(Locale.US))
                                    .map(d -> d.matches(filter.getDomain()))
                                    .orElse(false);

                            if (found) {
                                found = path
                                        .toLowerCase(Locale.US)
                                        .matches(filter.getPath());
                            }

                            if (found) {
                                break; // for
                            }
                        }

                        return found;
                    })
                    .orElse(false);
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        else {
            // Domains filter --domain option
            String domainSubstring = Property.FILTER_DOMAIN.value().orElse("")
                    .toLowerCase(Locale.US);
            String[] domains = domainSubstring.split(",", -1);
            logger.info("-- main() - arg domain substring filter: {}", Property.FILTER_DOMAIN.value());

            domainFilter = domain ->
            {
                boolean found = false;

                for (String domainValue : domains) {
                    found = domain
                            .map(d -> d.toLowerCase(Locale.US))
                            .map(d -> d.contains(domainValue))
                            .orElse(false);

                    if (found)
                    {
                        break; // for
                    }
                }

                return found;
            };

            // Filename extension filter.
            String filenameExtension = Property.FILTER_EXTENSION.value().orElse("")
                    .toLowerCase(Locale.US);
            logger.info("-- main() - arg filename extension filter: {}", Property.FILTER_EXTENSION.value());

            assetFilter = asset -> asset
                    .relativePath()
                    .map(d -> d.toLowerCase(Locale.US))
                    .map(d -> d.endsWith(filenameExtension))
                    .orElse(false);
        }


        List<String> files = Assets.files(assetsList, domainFilter);
        logger.info("-- main() - domain filtered file count: {}", files.size());

        // Output folders.
        Path outputFolder = Paths.get(Property.OUTPUT_FOLDER.value().orElse("")).resolve(device.serialNumber());
        Path assetOutputFolder = outputFolder.resolve("assets"); // TODO assets value injection
        Path chunkOutputFolder = outputFolder.resolve("chunks"); // TODO chunks value injection
        logger.info("-- main() - output folder chunks: {}", chunkOutputFolder);
        logger.info("-- main() - output folder assets: {}", assetOutputFolder);

        JSONObject jsonObj = new JSONObject();
        jsonObj.put("serialNumber", device.serialNumber());
        String deviceName = device.deviceName();

        if (deviceName.isEmpty())
        {
            deviceName = snapshot.deviceName();
        }

        jsonObj.put("deviceName", deviceName);
        jsonObj.put("version", snapshot.productVersion());
        jsonObj.put("timestamp", snapshot.modifiedDateInstant().toString());

        Path jsonPath = outputFolder.resolve("info.json").toAbsolutePath();
        FileUtils.createDirectories(jsonPath, logger);

        File jsonFile = new File(jsonPath.toString());
        jsonFile.createNewFile();
        FileWriter writer = new FileWriter(jsonFile);
        writer.write(jsonObj.toJSONString());
        writer.flush();
        writer.close();

        // Download tools.
        AuthorizeAssets authorizeAssets = AuthorizeAssets.backupd();
        DiskChunkStore chunkStore = new DiskChunkStore(chunkOutputFolder);
        StandardChunkEngine chunkEngine = new StandardChunkEngine(chunkStore);
        AssetDownloader assetDownloader = new AssetDownloader(chunkEngine);
        KeyBagManager keyBagManager = backup.newKeyBagManager();

        // Mystery Moo. 
        Moo moo = new Moo(authorizeAssets, assetDownloader, keyBagManager);

        // Batch process files in groups of 100.
        // TODO group files into batches based on file size.
        List<List<String>> batches = ListUtils.partition(files, 100);

        for (List<String> batch : batches) {
            List<Asset> assets = backup.assets(httpClient, batch)
                    .stream()
                    .filter(assetFilter::test)
                    .collect(Collectors.toList());
            logger.info("-- main() - filtered asset count: {}", assets.size());
            moo.download(httpClient, assets, assetOutputFolder);
        }
    }
}
// TODO time expired tokens / badly adjusted system clocks.
// TODO handle D in files
// TODO test that 0 is really 0, something doesn't seem quite right about it
// TODO file timestamp
// TODO filtering
// TODO concurrent downloads
// TODO everything else
