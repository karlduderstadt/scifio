/*
 * #%L
 * SCIFIO library for reading and converting scientific file formats.
 * %%
 * Copyright (C) 2011 - 2013 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package io.scif.formats.tiff;

import io.scif.FormatException;
import io.scif.common.DataTools;

import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 * Default service for working with TIFF files.
 *
 * @author Curtis Rueden
 */
@Plugin(type = Service.class)
public class DefaultTiffService extends AbstractService implements TiffService {

  @Parameter
  private LogService log;

  public void difference(byte[] input, IFD ifd) throws FormatException {
    int predictor = ifd.getIFDIntValue(IFD.PREDICTOR, 1);
    if (predictor == 2) {
      log.debug("performing horizontal differencing");
      int[] bitsPerSample = ifd.getBitsPerSample();
      long width = ifd.getImageWidth();
      boolean little = ifd.isLittleEndian();
      int planarConfig = ifd.getPlanarConfiguration();
      int bytes = ifd.getBytesPerSample()[0];
      int len = bytes * (planarConfig == 2 ? 1 : bitsPerSample.length);

      for (int b=input.length-bytes; b>=0; b-=bytes) {
        if (b / len % width == 0) continue;
        int value = DataTools.bytesToInt(input, b, bytes, little);
        value -= DataTools.bytesToInt(input, b - len, bytes, little);
        DataTools.unpackBytes(value, input, b, bytes, little);
      }
    }
    else if (predictor != 1) {
      throw new FormatException("Unknown Predictor (" + predictor + ")");
    }
  }

  public void undifference(byte[] input, IFD ifd) throws FormatException {
    int predictor = ifd.getIFDIntValue(IFD.PREDICTOR, 1);
    if (predictor == 2) {
      log.debug("reversing horizontal differencing");
      int[] bitsPerSample = ifd.getBitsPerSample();
      int len = bitsPerSample.length;
      long width = ifd.getImageWidth();
      boolean little = ifd.isLittleEndian();
      int planarConfig = ifd.getPlanarConfiguration();

      int bytes = ifd.getBytesPerSample()[0];

      if (planarConfig == 2 || bitsPerSample[len - 1] == 0) len = 1;
      len *= bytes;

      for (int b=0; b<=input.length-bytes; b+=bytes) {
        if (b / len % width == 0) continue;
        int value = DataTools.bytesToInt(input, b, bytes, little);
        value += DataTools.bytesToInt(input, b - len, bytes, little);
        DataTools.unpackBytes(value, input, b, bytes, little);
      }
    }
    else if (predictor != 1) {
      throw new FormatException("Unknown Predictor (" + predictor + ")");
    }
  }

}
