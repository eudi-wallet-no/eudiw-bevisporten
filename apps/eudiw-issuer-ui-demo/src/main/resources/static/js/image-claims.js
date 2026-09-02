/**
 * Shared image-claim policy for Bevisgenerator UI surfaces.
 */
window.imageClaims = {
  isImageClaim(path) {
    return ['portrait', 'portrait_image', 'image'].includes(path);
  },

  mimeType(mimeType) {
    return mimeType || 'image/png';
  },

  dataUrl(value, mimeType) {
    return `data:${this.mimeType(mimeType)};base64,${value}`;
  }
};
