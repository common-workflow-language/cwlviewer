package org.commonwl.viewer.researchobject;

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

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (orcid != null ? !orcid.equals(that.orcid) : that.orcid != null) return false;
        return uri != null ? uri.equals(that.uri) : that.uri == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (orcid != null ? orcid.hashCode() : 0);
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        return result;
    }

}
