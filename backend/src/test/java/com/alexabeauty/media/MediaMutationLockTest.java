package com.alexabeauty.media;

import com.alexabeauty.catalog.domain.*;
import com.alexabeauty.catalog.repository.*;
import com.alexabeauty.common.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MediaMutationLockTest {
 @Test void everyImageMutationLocksParentBeforeImageLookup() {
  for(boolean cover : new boolean[]{false,true}) {
   var products=mock(ProductRepository.class);var images=mock(ProductImageRepository.class);var storage=mock(StorageGateway.class);
   UUID id=UUID.randomUUID(),imageId=UUID.randomUUID();var product=Product.builder().id(id).build();var image=ProductImage.builder().id(imageId).product(product).storagePath("image.png").build();
   when(products.findByIdForUpdate(id)).thenReturn(Optional.of(product));when(images.findById(imageId)).thenReturn(Optional.of(image));when(images.findByProductId(id)).thenReturn(List.of(image));
   var service=new MediaServiceImpl(storage,products,images);if(cover)service.setCoverImage(id,imageId);else service.deleteProductImage(id,imageId);
   var order=inOrder(products,images);order.verify(products).findByIdForUpdate(id);order.verify(images).findById(imageId);
  }
 }
 @Test void terminalProductCannotChangeCoverOrDeleteHistoricalImage() {
  var products=mock(ProductRepository.class);var images=mock(ProductImageRepository.class);var storage=mock(StorageGateway.class);UUID id=UUID.randomUUID();
  when(products.findByIdForUpdate(id)).thenReturn(Optional.of(Product.builder().id(id).deletedAt(java.time.Instant.now()).build()));var service=new MediaServiceImpl(storage,products,images);
  assertThatThrownBy(()->service.setCoverImage(id,UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
  assertThatThrownBy(()->service.deleteProductImage(id,UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
  verifyNoInteractions(images,storage);
 }
}
