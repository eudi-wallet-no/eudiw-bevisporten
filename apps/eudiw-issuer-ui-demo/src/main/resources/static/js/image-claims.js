/**
 * Shared image-claim policy for Bevisgenerator UI surfaces.
 */
window.imageClaims = {
  isImageClaim(path) {
    return ['portrait', 'portrait_image', 'image'].includes(path);
  },

  mimeType(mimeType, value) {
    if (mimeType) return mimeType;
    if (typeof value === 'string' && value.startsWith('/9j/')) return 'image/jpeg';
    if (typeof value === 'string' && value.startsWith('UklGR')) return 'image/webp';
    return 'image/png';
  },

  dataUrl(value, mimeType) {
    if (typeof value === 'string' && /^data:image\/(?:png|jpe?g|webp)(?:;[^,]*)?,/i.test(value)) {
      return value;
    }
    return `data:${this.mimeType(mimeType, value)};base64,${value}`;
  }
};
