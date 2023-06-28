/*
 * Copyright (C) 2018 The ontology Authors
 * This file is part of The ontology library.
 *
 *  The ontology is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  The ontology is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with The ontology.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.github.ontio.network.rpc;

import com.alibaba.fastjson.JSON;
import com.github.ontio.common.ErrorCode;
import com.github.ontio.network.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class Interfaces {
    private final Logger log = LoggerFactory.getLogger(Interfaces.class);
    private final URL url;


    public Interfaces(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    private static double getNextId() {
        double d = 0.0;
        do {
            d = Math.random();
        } while (("" + d).indexOf("E") != -1);
        return d;
    }

    public String getHost() {
        return url.getHost() + " " + url.getPort();
    }

    public Object call(String method, Object... params) throws RpcException, IOException {
        Map req = makeRequest(method, params);
        Map response = (Map) send(req);
        if (response == null) {
            throw new RpcException(0, ErrorCode.ConnectUrlErr(url + "response is null. maybe is connect error"));
        } else if ((int) response.get("error") == 0) {
            return response.get("result");
        } else {
            throw new RpcException(0, JSON.toJSONString(response));
        }
    }

    private Map makeRequest(String method, Object[] params) {
        Map request = new HashMap();
        request.put("jsonrpc", "2.0");
        request.put("method", method);
        request.put("params", params);
        request.put("id", 1);
        log.debug("POST url={},{}", this.url, JSON.toJSONString(request));
        return request;
    }


    public Object send(Object request) throws IOException {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            //set parameter type to JSON format
            connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            try (OutputStreamWriter w = new OutputStreamWriter(connection.getOutputStream())) {
                w.write(JSON.toJSONString(request));
            }
            try (InputStreamReader r = new InputStreamReader(connection.getInputStream())) {
                StringBuffer temp = new StringBuffer();
                int c = 0;
                while ((c = r.read()) != -1) {
                    temp.append((char) c);
                }
                log.debug("result==>>>{}", temp.toString());
                return JSON.parseObject(temp.toString(), Map.class);
            }
        } catch (IOException e) {
        }
        return null;
    }
}
