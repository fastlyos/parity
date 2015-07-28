package org.jvirtanen.parity.system;

import static org.jvirtanen.parity.util.Applications.*;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.jvirtanen.config.Configs;

class TradingSystem {

    public static final long EPOCH_MILLIS = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
        .toInstant().toEpochMilli();

    public static void main(String[] args) throws IOException {
        if (args.length != 1)
            usage("parity-system <configuration-file>");

        try {
            main(config(args[0]));
        } catch (ConfigException | FileNotFoundException e) {
            error(e);
        }
    }

    private static void main(Config config) throws IOException {
        MarketDataServer marketData = marketData(config);

        String      marketReportSession        = config.getString("market-report.session");
        InetAddress marketReportMulticastGroup = Configs.getInetAddress(config, "market-report.multicast-group");
        int         marketReportMulticastPort  = Configs.getPort(config, "market-report.multicast-port");
        int         marketReportRequestPort    = Configs.getPort(config, "market-report.request-port");

        MarketReportServer marketReport = MarketReportServer.create(marketReportSession,
                new InetSocketAddress(marketReportMulticastGroup, marketReportMulticastPort),
                marketReportRequestPort);

        List<String> instruments = config.getStringList("instruments");

        MatchingEngine engine = new MatchingEngine(instruments, marketData, marketReport);

        int orderEntryPort = Configs.getPort(config, "order-entry.port");

        OrderEntryServer orderEntry = OrderEntryServer.create(orderEntryPort, engine);

        marketData.version();

        new Events(marketData, marketReport, orderEntry).run();
    }

    private static MarketDataServer marketData(Config config) throws IOException {
        String      session        = config.getString("market-data.session");
        InetAddress multicastGroup = Configs.getInetAddress(config, "market-data.multicast-group");
        int         multicastPort  = Configs.getPort(config, "market-data.multicast-port");
        int         requestPort    = Configs.getPort(config, "market-data.request-port");

        return MarketDataServer.create(session, new InetSocketAddress(multicastGroup, multicastPort),
                requestPort);
    }

}
