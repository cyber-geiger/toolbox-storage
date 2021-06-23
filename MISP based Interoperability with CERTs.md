# **Conversation Module data manipulation and aggregation:**

At the end of the conversation between the client and the chatbot, the conversation content is delivered to the Proxy Module in a JSON format, where the data is extracted and manipulated.
The JSON received by the chatbot is analyzed by the Proxy Module processing engine in order to extract the relevant information and transfer it to the Atos API.

**The Proxy Module sends the JSON file to the ATOS API by the following JSON format:**


    {
      "info": String,
      "level": INT,
      "date": DATE,
      "distribution": INT,
      "tag": STRING,
      "analysis": INT,
      "attributes": [
        {
          "category": STRING,
          "type": STRING,
          "value": STRING
        }
      ]
    }

**MISP Event Model**
MISP events are encapsulations for contextually related information represented as attribute and object.

| Description| info| level| date| distribution| tag| analysis| attribute|
| --- | --- | --- | --- | --- | --- | --- | --- |
| `Type` | String | Integer | String | Integer | String | Integer | MISP Attribute JSON Format (see below) |
| `Required` | True |  |  |  |  |  |  |
| `Description` | Description of the event| Event's security threat level. Integer value from 1 to 4, in which 1 is High and 4 is Undefined | Event's date of occurrence in format 'YYYY-MM-DD' | Event's distribution level. An integer value in which 0 means 'Your organization only', 1 means 'This community only', 2 means 'Connected communities', and 3 means 'All communities' | Tags that help MISP to relate and group attributes and events | Event's analysis status. An integer value in which: 0 means 'Initial', 1 means 'Ongoing' and 2 means 'Completed' | JSON that contains the attribute's information of the event |

**MISP Attribute Model**
Attributes in MISP can be network indicators (e.g. IP address), system indicators (e.g. a string in memory) or even bank account details.

| Description| category| type| value|
| --- | --- | --- | --- |
| `Type` | String | String | String |
| `Required` | True | True | True |
| `Description` | Attribute's Category - See attached JSON file with the mapping Category/Type for more info | Attribute's Type - See attached JSON file with the mapping Category/Type for more info | Information, attribute's details, e.g. IP address, URL, etc. Depend on the Type of Attribute |


Both the conversation manipulated data and the client's specific information (under the GDPR privacy constraints) are stored locally and afterwards in a scheduled time it stores this data in the GEIGER cloud by using the Data Cloud Sync component.

## The Proxy Module receives a JSON file with the following content:

    {
      "Event": {
        "threat_level_id": "3",
        "date": "2020-09-16",
        "timestamp": "1600229213",
        "analysis": "2",
        "info": "Daily Incremental Cryptolaemus Emotet IOCs (payload)",
        "extends_uuid": "",
        "publish_timestamp": "1600250505",
        "uuid": "1120e5fb-e39a-41ff-a0e7-0ac04545b09e",
        "published": true,
        "Orgc": {
          "name": "CERT-BUND",
          "uuid": "56a64d7a-63dc-4471-bce9-4accc25ed029"
        },
        "Tag": [
          {
            "colour": "#ad00ff",
            "name": "malware:emotet"
          },
          {
            "color": "#ffffff",
            "name": "tlp:white"
          },
          {
            "colour": "#0088cc",
            "name": "misp-galaxy:malpedia=\"Geodo\""
          },
          {
            "colour": "#0088cc",
            "name": "misp-galaxy:tool=\"Emotet\""
          }
        ],
        "Attribute": [
          {
            "value": "http://0931tangfc.com/config/paclm/ekw50pjaxptd/",
            "deleted": false,
            "comment": "Daily Cryptolaemus Import (emotet IOC update)",
            "timestamp": "1600229166",
            "type": "url",
            "to_ids": true,
            "category": "Network activity",
            "uuid": "05835825-4b0c-4371-8aea-f367e560dd6c",
            "disable_correlation": false,
            "Tag": [
              {
                "colour": "#ff0099",
                "name": "emotet:epoch=\"2\""
              }
            ]
          },
          {
            "value": "http://aboveandbelow.com.au/cgi-bin/h4i4ol3r3/",
            "deleted": false,
            "comment": "Daily Cryptolaemus Import (emotet IOC update)",
            "timestamp": "1600229166",
            "type": "url",
            "to_ids": true,
            "category": "Network activity",
            "uuid": "09ab28e3-516a-407d-a6d1-98c4056d662d",
            "disable_correlation": false,
            "Tag": [
              {
                "colour": "#ff0099",
                "name": "emotet:epoch=\"2\""
              }
            ]
          },
          {
            "value": "http://annial.com/wp-admin/eTrac/rttxtco1499852409513214myq2w4ab/",
            "deleted": false,
            "comment": "Daily Cryptolaemus Import (emotet IOC update)",
            "timestamp": "1600229166",
            "type": "url",
            "to_ids": true,
            "category": "Network activity",
            "uuid": "060ff5e6-ff46-4ab2-b20c-63cbc848b5e2",
            "disable_correlation": false,
            "Tag": [
              {
                "colour": "#ff0099",
                "name": "emotet:epoch=\"2\""
              }
            ]
          },
        }
    }


# **Mapping Module**
The mapping component was developed to match between taxonomies of the Geiger and the CERT-RO. The process works in both ways, from Geiger to CERT-RO and vice versa.  Technical side: the service works as REST API, running on Azure App Service and always live to get requests and return responses. Calls can be sent to the same HTTP URL, and the service will know to classify whether it's a Geiger to CERT or CERT to Geiger.  Requests need to be sent with a key and a value:
* Key: can be either **misp_taxonomy** or **geiger_taxonomy**
* Value can be any value that was pre-defined in the relevant key (misp or geiger)
Then, the service matches the value on the right key-table and returns the correct value from the other key-table, together with the original request.

Example for a CERT-RO to Geiger call:
```javascript
{
    "misp_taxonomy":"CERT-RO:compromised-resources=\"compromised-website\""
}
```
Response:
```JSON
{
    "geiger_taxonomy": "CERT-GEIGER:compromised-resources=\"Web-based threats\"",
    "misp_taxonomy": "CERT-RO:compromised-resources=\"compromised-website\""
}
```
URL: https://mapping-geiger.azurewebsites.net  We can send the request with the key **geiger_taxonomy** and relevant value instead of **misp_taxonomy ** to get the other direction response.  The service is scalable and easy to expand so more values can be added easily to both tables.






