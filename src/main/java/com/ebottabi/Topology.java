package com.ebottabi;

import com.ebottabi.bolt.TwitterFilterBolt;
import com.ebottabi.spout.TwitterSpout;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import com.ebottabi.bolt.LinkFilterBolt;
import com.ebottabi.bolt.RedisGooseExtractor;
import com.ebottabi.bolt.RedisLinksPublisherBolt;
import com.ebottabi.bolt.RedisMarketBolt;
import com.ebottabi.bolt.RedisRetweetBolt;
import com.ebottabi.bolt.RedisTagsPublisherBolt;

public class Topology {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        TopologyBuilder builder = new TopologyBuilder();

        //Tweets from twitter sport
        //TODO: setup your twitter credentials
        TwitterSpout twitterSpout = new TwitterSpout();
        builder.setSpout("twitter", twitterSpout);

        //Initial filter
        builder.setBolt("filter", new TwitterFilterBolt(), 2).shuffleGrouping("twitter");

        //Tags publishing
        builder.setBolt("tags", new RedisTagsPublisherBolt("tags")).shuffleGrouping("filter");

        //Retweets
        builder.setBolt("retweets", new RedisRetweetBolt(3), 2).shuffleGrouping("filter");

        //Links
        builder.setBolt("linkFilter", new LinkFilterBolt(), 2).shuffleGrouping("filter");
        builder.setBolt("links", new RedisLinksPublisherBolt(), 4).shuffleGrouping("linkFilter");
        builder.setBolt("market", new RedisMarketBolt(), 1).shuffleGrouping("links");
        builder.setBolt("articles", new RedisGooseExtractor(), 5).shuffleGrouping("retweets");


        Config conf = new Config();
        conf.setDebug(false);

        if (args != null && args.length > 0) {
            conf.setNumWorkers(3);

            StormSubmitter.submitTopology(args[0], conf, builder.createTopology());
        } else {
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("twitter", conf, builder.createTopology());
        }

    }
}