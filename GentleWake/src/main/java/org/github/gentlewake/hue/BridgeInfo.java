/* TODO: license */
package org.github.gentlewake.hue;

/**
 * The Hue broker service used in {@link org.github.gentlewake.hue.RetrieveBridgeIpTask} returns objects ob this type.
 *
 * @author lorenz.fischer@gmail.com
 */
public class BridgeInfo {

    private String id;
    private String internalipaddress;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInternalipaddress() {
        return internalipaddress;
    }

    public void setInternalipaddress(String internalipaddress) {
        this.internalipaddress = internalipaddress;
    }




}
