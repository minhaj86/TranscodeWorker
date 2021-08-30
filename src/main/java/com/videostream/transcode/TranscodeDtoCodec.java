package com.videostream.transcode;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class TranscodeDtoCodec implements MessageCodec<TranscodeJobDTO, TranscodeJobDTO> {
    @Override
    public void encodeToWire(Buffer buffer, TranscodeJobDTO transcodeJobDTO) {

    }

    @Override
    public TranscodeJobDTO decodeFromWire(int pos, Buffer buffer) {
        return null;
    }

    @Override
    public TranscodeJobDTO transform(TranscodeJobDTO transcodeJobDTO) {
        return transcodeJobDTO;
    }

    @Override
    public String name() {
        return TranscodeJobDTO.class.getSimpleName();
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
