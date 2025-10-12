package org.dreamcat.daily.script;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.elasticsearch.EsDocClient;
import org.dreamcat.common.elasticsearch.EsIndexClient;
import org.dreamcat.common.elasticsearch.EsQueryClient;
import org.dreamcat.common.elasticsearch.EsRestClientUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2023-08-19
 */
@ArgParserType(allProperties = true)
public class EsModule {

    private String url;
    private String user;
    private String password;

    transient EsIndexClient esIndexClient;
    transient EsQueryClient esQueryClient;
    transient EsDocClient esDocClient;

    protected void afterPropertySet() throws Exception {
        List<String> urls;
        if (!url.contains(",")) {
            urls = Collections.singletonList(url);
        } else {
            urls = Arrays.stream(url.split(",")).collect(Collectors.toList());
        }
        ElasticsearchClient client = EsRestClientUtil.elasticsearchClient(urls, user, password);

        esIndexClient = new EsIndexClient(client);
        esQueryClient = new EsQueryClient(client);
        esDocClient = new EsDocClient(client);
    }
}
