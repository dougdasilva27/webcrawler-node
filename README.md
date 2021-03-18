# Webcrawler Node 

Webcrawler node is a Java Project containing all the crawlers from Lett.

## Index
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
        - [Install Java](#install-java)
        - [Install Eclipse](#install-eclipse)
        - [VPN connection](#vpn-connection)
        - [Configuration files](#configuration-files)
        - [Clone project](#clone-project)
        - [Configure maven](#configure-maven)
- [Launch Configurations](#launch-configurations)

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

What things you need to install the software and how to install them

#### Install Java
1 - Open a terminal (Ctrl-Alt-T) and switch it to root permissions by entering:

```bash
$ sudo su
```

2 - Make sure Eclipse Indigo is NOT installed in your Ubuntu. You may need to remove both "eclipse" and "eclipse-platform" packages to get rid of it. If it still gets into way when trying to install Luna using this easy way, you may need to look at the "hard way" below.

```bash
# apt-get remove eclipse eclipse-platform
```

3 - Install a Java 1.8 JDK:

```bash
# apt-get update
# apt-get install openjdk-8-jdk
```

4 - Install Maven:

```bash
# apt-get install maven
```

5 - Get rid of the root access as you won't need it anymore:

```bash
# exit
```

---
#### Install Eclipse

1 - Download Eclipse Oxygen. The "for Java EE Developers" or "for Java Developers" versions all seem to work. 
Currently the file which was tested to work is (note that it is for 64 bit Ubuntu version) available at this page

2 - Extract the Eclipse installation tarball into your home directory:

```bash
$ cd
$ tar -xzvf <path/to/your-tar-file>
```

3 - Run Eclipse:

```bash
$ ~/eclipse-installer/eclipse-inst
```

---
#### VPN connection

Before running the application, you need to connect to our VPN. If you do not know how to do it, talk with the Team Leader.

---
#### Configuration files

You need to create a file named settings.xml. This file is for download maven dependencies.
To create this file, you will need AWS credentials (search in your vault manager for _aws programmatic_ or ask for your leader).

After that you need to add that file to your home's .m2 folder.

`$HOME/.m2/settings.xml` example:

```xml
<?xml version="1.0"?>

<settings xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/SETTINGS/1.0.0">
    <servers>
        <server>
            <id>lett-maven</id>
            <username>{{INSERT YOUR USERNAME HERE}}</username>
            <password>{{INSERT YOUR PASSWORD HERE}}</password>
        </server>
    </servers>
</settings>
```
> NOTE: You need read permissions on AWS IAM to read lett-maven's bucket to download maven dependencies

---
#### Clone project

```bash
# git clone git@github.com:lettdigital/webcrawler-node.git
```

---
#### Configure maven

In your eclipse, set your project for a maven project, after that the dependencies will be downloaded.

Or you can run it in terminal:
```bash
# nvm clean install -U
```
> NOTE: This command will clean and force install maven dependencies.


---
## Launch Configurations

You can ask your team leader for this configurations, after that you need to change the credentials
of the environment variables to your credentials, like postgres, mongo and aws.

But you can test localy configuring a application debug in your IDE.

Here below you see how to config in Intellij IDEA, but is very similar in others IDEs:

![image](https://user-images.githubusercontent.com/12951402/96016770-869ad080-0e1f-11eb-9e61-efddcf04fb41.png)

1 - First create a application debugger, in can find under `run/debug`

2 - Set the main class: `br.com.lett.crawlernode.test.Test`

3 - Define program arguments:
```bash
-city {{MARKET CITY}} -market {{MARKET NAME}} -testType [keyword|insights]
```
> NOTE: The test type can be 'keywords' for ranking crawler or 'insights' for crawler core

> NOTE 2: If you set insights, you need change product URL in the [Test class](https://github.com/lettdigital/webcrawler-node/blob/master/src/java/br/com/lett/crawlernode/test/Test.java#L105) to correct scrap the page

4 - Fill the {{USERNAME}} and {{PASSWORD}} from the example below and paste to the Environment variables on IDE.

Example:
```txt
CRAWLER_THREADS=1;
DEBUG=ON;
ENVIRONMENT=production;
FETCHER_URL=https://api-fetcher.lett.global/;
HIKARI_CP_CONNECTION_TIMEOUT=60000;
HIKARI_CP_IDLE_TIMEOUT=20000;
HIKARI_CP_MAX_POOL_SIZE=3;
HIKARI_CP_MIN_IDLE=2;
HIKARI_CP_VALIDATION_TIMEOUT=5000;
KINESIS_STREAM=sku-core-crawler-kinesis-stream;
LETT_MONGO_FETCHER_DATABASE=fetcher;
LETT_MONGO_FETCHER_HOST=mongodb0.lett.global,mongodb1.lett.global,mongodb2.lett.global;
LETT_MONGO_FETCHER_PASSWORD={{PASSWORD}};
LETT_MONGO_FETCHER_PORT=27017;
LETT_MONGO_FETCHER_USERNAME={{USERNAME}};
LETT_MONGO_FROZEN_DATABASE=frozen;
LETT_MONGO_FROZEN_HOST=mongodb0.lett.global,mongodb1.lett.global,mongodb2.lett.global;
LETT_MONGO_FROZEN_PASSWORD={{PASSWORD}};
LETT_MONGO_FROZEN_PORT=27017;
LETT_MONGO_FROZEN_USERNAME={{USERNAME}};
LETT_MONGO_INSIGHTS_DATABASE=insights;
LETT_MONGO_INSIGHTS_HOST=mongodb0.lett.global,mongodb1.lett.global,mongodb2.lett.global;
LETT_MONGO_INSIGHTS_PASSWORD={{PASSWORD}};
LETT_MONGO_INSIGHTS_PORT=27017;
LETT_MONGO_INSIGHTS_USERNAME={{USERNAME}};
LETT_MONGO_PANEL_DATABASE=panel;
LETT_MONGO_PANEL_HOST=localhost;
LETT_MONGO_PANEL_PASSWORD={{PASSWORD}}pass;
LETT_MONGO_PANEL_PORT=27017;
LETT_MONGO_PANEL_USERNAME={{USERNAME}};
LETT_POSTGRES_DATABASE=products;
LETT_POSTGRES_HOST=postgres-prod.lett.global;
LETT_POSTGRES_PASSWORD={{PASSWORD}};
LETT_POSTGRES_PORT=5432;
LETT_POSTGRES_USERNAME={{USERNAME}};
LOGS_BUCKET_NAME=lett-webscraper-htmls-dev;
TMP_IMG_FOLDER=$HOME/lett/images;
HTML_PATH=$HOME/htmls-crawler/;
USE_FETCHER=false;
S3_BATCH_USER={{USERNAME}};
S3_BATCH_HOST=s3-batch.lett.global;
S3_BATCH_REMOTE_LOCATION=/s3_buckets/batch;
SSH_KEYS_BUCKET=lett-ssh-keys;
S3_BATCH_PASS={{PASSWORD}};
ATTEMPTS_FOR_EACH_PROXY=2;
CHROME_PATH=$HOME/chromeDrive/chromedriver
```

## Running the tests

https://www.notion.so/lett/Crawler-1d6ce7a0dee94de686c8f2458a3f810f


### And coding style tests

https://www.notion.so/lett/Java-Style-Guideline-512c01ec2de04d77a3af73e78e9005eb

## Deployment

Use tag [deploy:Production] in your commit for deploy changes, but this only can be done on branch master

## Authors

Fabricio Massula
Samir Leão
Gabriel Dornelas

## Acknowledgments

* REAMDE.md template from [PurpleBooth](https://gist.githubusercontent.com/PurpleBooth/109311bb0361f32d87a2/raw/8254b53ab8dcb18afc64287aaddd9e5b6059f880/README-Template.md)
