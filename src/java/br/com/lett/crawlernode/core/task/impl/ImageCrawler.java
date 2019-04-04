package br.com.lett.crawlernode.core.task.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.services.s3.model.ObjectMetadata;
import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.imgprocessing.ImageConverter;
import br.com.lett.crawlernode.core.imgprocessing.ImageDownloadResult;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.session.crawler.ImageCrawlerSession;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class ImageCrawler extends Task {

  private static final Logger LOGGER = LoggerFactory.getLogger(ImageCrawler.class);

  private final int IMAGE_CHECKING_TRY = 5;

  public ImageCrawler(Session session) {
    this.session = session;
  }

  @Override
  public void processTask() {

    Logging.printLogDebug(LOGGER, session, "Downloading image for processed:" + session.getProcessedId());

    try {
      ImageDownloadResult simpleDownloadResult = simpleDownload();

      Logging.printLogInfo(LOGGER, session, "Downloaded file md5: " + simpleDownloadResult.md5);
      Logging.printLogInfo(LOGGER, session, "Image file format: " + simpleDownloadResult.imageFormat);

      if (simpleDownloadResult.imageFile == null) {
        Logging.printLogError(LOGGER, session,
            "Failed to download image " + session.getOriginalURL() + " ....returning from image processTask()....");
        session.registerError(new SessionError(SessionError.BUSINESS_LOGIC, "Download image failed."));
        return;
      }

      // get metadata from the image on Amazon
      Logging.printLogDebug(LOGGER, session,
          "Fetching image object metadata on Amazon: " + ((ImageCrawlerSession) session).getTransformedImageKeyOnBucket());
      ObjectMetadata metadata = S3Service.fetchObjectMetadata(session, ((ImageCrawlerSession) session).getTransformedImageKeyOnBucket());

      if (metadata == null) { // we doesn't have any image under this path in S3 yet
        Logging.printLogDebug(LOGGER, session, "This image isn't on Amazon yet.");

        if (simpleDownloadResult.imageFile != null && simpleDownloadResult.md5 != null) {
          update(simpleDownloadResult);
        }
      }

      // we already have an image under this path in S3
      else {

        String amazonMd5 = metadata.getUserMetaDataOf(S3Service.MD5_ORIGINAL_HEX_FIELD);

        Logging.printLogDebug(LOGGER, session,
            "Looking for change on image # " + ((ImageCrawlerSession) session).getImageNumber() + "... (first check)");
        if (amazonMd5 == null) {
          Logging.printLogDebug(LOGGER, session, "Amazon MD5 doesn't exists yet.");
        } else {
          Logging.printLogDebug(LOGGER, session, "Amazon MD5: " + amazonMd5);
        }
        Logging.printLogDebug(LOGGER, session, "Local MD5: " + simpleDownloadResult.md5);

        // let's see if the md5 has changed
        if (amazonMd5 == null || isDifferent(amazonMd5, simpleDownloadResult.md5)) {
          Logging.printLogDebug(LOGGER, session, "The new md5 doesn't exists on Amazon yet, or it's different from the previous md5.");

          ImageDownloadResult finalDownloadResult;

          if (amazonMd5 != null) {
            finalDownloadResult = trucoDownload(amazonMd5, simpleDownloadResult.md5);
          } else {
            finalDownloadResult = simpleDownloadResult;
          }

          update(finalDownloadResult);
        } else if (GlobalConfigurations.executionParameters.mustForceImageUpdate()) {
          Logging.printLogDebug(LOGGER, session, "The image md5 is already on Amazon, but i want to force the update.");
          update(simpleDownloadResult);
        } else {
          Logging.printLogDebug(LOGGER, session, "The image md5 is already on Amazon.");
        }
      }

    } catch (Exception e) {
      session.registerError(new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)));
      Logging.printLogError(LOGGER, session, CommonMethods.getStackTraceString(e));
    }

  }

  @Override
  public void onStart() {
    Logging.printLogDebug(LOGGER, session, "START");
  }

  @Override
  public void onFinish() {
    List<SessionError> errors = session.getErrors();

    if (!errors.isEmpty()) {
      Logging.printLogWarn(LOGGER, session, "Task failed!");
      session.setTaskStatus(Task.STATUS_FAILED);
    } else { // only remove the task from queue if it was flawless
      Logging.printLogDebug(LOGGER, session, "Task completed.");
      session.setTaskStatus(Task.STATUS_COMPLETED);
    }

    // clear the session
    session.clearSession();
    Logging.printLogDebug(LOGGER, session, "END");
  }

  /**
   * This method takes the following steps: <br>
   * Creates a transformed version of the image. If necessary, converts to JPG. An transformed version
   * is always created, even if the image is originally a JPG. <br>
   * Upload all the content to the Amazon bucket
   * 
   * @param imageDownloadResult
   * @throws IOException
   */
  private void update(ImageDownloadResult imageDownloadResult) throws IOException {
    ImageCrawlerSession imageCrawlerSession = (ImageCrawlerSession) session;

    // upload the transformed version of the image to Amazon
    File transformedImageFile = ImageConverter.createTransformedImageFile(imageDownloadResult.imageFile, session);

    if (transformedImageFile != null) {
      Logging.printLogDebug(LOGGER, session, "Uploading transformed image to Amazon ... ");
      String transformedImageFileMd5 = CommonMethods.computeMD5(transformedImageFile);
      ObjectMetadata transformedImageMetadata = new ObjectMetadata();
      transformedImageMetadata.addUserMetadata(S3Service.MD5_HEX_METADATA_FIELD, transformedImageFileMd5);
      transformedImageMetadata.addUserMetadata(S3Service.MD5_ORIGINAL_HEX_FIELD, imageDownloadResult.md5); // also put the md5 of the original image
      S3Service.uploadImage(session, transformedImageMetadata, transformedImageFile, imageCrawlerSession.getTransformedImageKeyOnBucket());
      Logging.printLogDebug(LOGGER, session, "Done ... ");
    } else {
      Logging.printLogWarn(LOGGER, session, "Transformed image file was not sent to S3 because it is null.");
    }

    // upload the original image to Amazon
    if (imageDownloadResult.md5 != null && !imageDownloadResult.imageFormat.isEmpty()) {
      Logging.printLogDebug(LOGGER, session, "Uploading original image to Amazon...");
      ObjectMetadata newObjectMetadata = new ObjectMetadata();
      newObjectMetadata.addUserMetadata(S3Service.MD5_HEX_METADATA_FIELD, imageDownloadResult.md5);
      S3Service.uploadImage(session, newObjectMetadata, new File(imageCrawlerSession.getLocalOriginalFileDir()),
          imageCrawlerSession.getOriginalImageKeyOnBucket());
      Logging.printLogDebug(LOGGER, session, "Done.");
    } else {
      Logging.printLogWarn(LOGGER, session, "Original image was not sent to S3 because it either has a null md5 or an unknown image format");
    }
  }

  private ImageDownloadResult simpleDownload() throws IOException {
    ImageDownloadResult result = new ImageDownloadResult();
    File imageFile = downloadImage();
    String md5 = CommonMethods.computeMD5(imageFile);
    String imageFormat = ImageConverter.getImageFormatName(imageFile, session);

    result.imageFile = imageFile;
    result.md5 = md5;
    result.imageFormat = imageFormat;

    return result;
  }

  private ImageDownloadResult trucoDownload(String amazonMd5, String currentMd5) throws IOException {
    String pastIterationMd5 = currentMd5;
    int iteration = 1;
    ImageDownloadResult result = null;

    Logging.printLogDebug(LOGGER, session, "Initiating truco download...");

    while (iteration <= IMAGE_CHECKING_TRY) {
      try {
        Logging.printLogDebug(LOGGER, session, "Iteration " + iteration + "...");

        // Fazer um novo download da imagem
        result = simpleDownload();

        // Comparar com a da iteração anterior, se der igual, retorno
        if (result.md5 != null && result.md5.equals(pastIterationMd5)) {
          Logging.printLogDebug(LOGGER, session, "Fiz um download novo e constatei que era igual a imagem que eu tinha da iteração anterior");
          return result;
        }

        // Comparar o md5 do download atual com o da Amazon -- se chegou aqui, significa que o download
        // atual não foi igual ao da iteração anterior
        if (amazonMd5.equals(result.md5)) {
          Logging.printLogDebug(LOGGER, session, "Imagem do download atual, que é a iteração " + iteration + " é igual a da Amazon.");
          return result;
        }

        // Se chegou até aqui significa que o novo download não foi igual ao download da iteração anterior e
        // que também
        // não foi igual ao da Amazon. Vamos atualizar o pastIterationMd5 com o download atual e partir para
        // a próxima iteração
        Logging.printLogDebug(LOGGER, session, "O novo download não foi igual ao download da iteração anterior.");
        Logging.printLogDebug(LOGGER, session, "Vou partir para a próxima iteração.");

        pastIterationMd5 = result.md5;

        iteration++;

      } catch (Exception e) {
        Logging.printLogError(LOGGER, session, CommonMethods.getStackTraceString(e));
      }
    }

    Logging.printLogDebug(LOGGER, session, "Fiz todas as tentativas e conclui que a imagem realmente mudou.");

    return result;
  }

  /**
   * 
   * @return
   * @throws IOException
   */
  private File downloadImage() throws IOException {
    Logging.printLogDebug(LOGGER, session, "Downloading image from market...");

    Request request = RequestBuilder.create().setUrl(session.getOriginalURL()).build();

    int marketId = session.getMarket().getNumber();
    Map<String, String> headers = new HashMap<>();

    if (marketId == 63 || marketId == 62 || marketId == 73) {
      request.setSendContentEncoding(false);

      headers.put(HttpHeaders.CONNECTION, "keep-alive");
      headers.put(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
      headers.put(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br");
      headers.put(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");

      switch (marketId) {
        case 63:
          headers.put(HttpHeaders.HOST, "www.pontofrio-imagens.com.br");
          break;
        case 62:
          headers.put(HttpHeaders.HOST, "www.casasbahia-imagens.com.br");
          break;
        case 73:
          headers.put(HttpHeaders.HOST, "www.extra-imagens.com.br");
          break;
        default:
          break;
      }
    } else if (marketId == 307) {
      headers.put(HttpHeaders.ACCEPT, "image/jpg, image/apng");
      request.setSendContentEncoding(false);
    }

    request.setHeaders(headers);

    return new ApacheDataFetcher().fetchImage(session, request);
  }


  /**
   * 
   * @param metadata
   * @param md5
   * @return
   */
  private boolean isDifferent(String amazonMd5, String md5) {

    // Se nunca teve imagem na amazon, retorna true se o md5 da local não for nulo também
    if (amazonMd5 == null) {
      return md5 != null;
    }

    // Se tem imagem na amazon, retorna true se o md5 da local for diferente ou nulo
    else {
      return !amazonMd5.equals(md5);
    }

  }

}
