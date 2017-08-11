/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.commonwl.view.researchobject;

import org.apache.taverna.robundle.manifest.Agent;

import java.net.URI;

/**
 * An implementation of Agent with added HashCode and Equals methods
 * for use in sets
 */
public class HashableAgent extends Agent {

    private String name;
    private URI orcid;
    private URI uri;

    public HashableAgent() {}

    public HashableAgent(String name, URI orcid, URI uri) {
        this.name = name;
        this.orcid = orcid;
        this.uri = uri;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public URI getOrcid() {
        return orcid;
    }

    @Override
    public void setOrcid(URI orcid) {
        this.orcid = orcid;
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public void setUri(URI uri) {
        this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass() || super.getClass() != o.getClass()) return false;

        HashableAgent that = (HashableAgent) o;

        // ORCID is a unique identifier so if matches, the objects are equal
        if (orcid != null && orcid.equals(that.orcid)) return true;

        // If no ORCID is present but email is the name, the objects are equal
        if (orcid == null && uri != null && uri.equals(that.uri)) return true;

        // Default to checking all parameters
        if (orcid != null ? !orcid.equals(that.orcid) : that.orcid != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return uri != null ? uri.equals(that.uri) : that.uri == null;

    }

    /**
     * ORCID is used as hashcode to fall back to comparison if missing
     * @return The hash code for this object
     */
    @Override
    public int hashCode() {
        return orcid != null ? orcid.hashCode() : 0;
    }

}
