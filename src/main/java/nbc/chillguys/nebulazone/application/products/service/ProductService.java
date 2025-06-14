package nbc.chillguys.nebulazone.application.products.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.application.auction.service.AuctionSchedulerService;
import nbc.chillguys.nebulazone.application.products.dto.request.ChangeToAuctionTypeRequest;
import nbc.chillguys.nebulazone.application.products.dto.request.CreateProductRequest;
import nbc.chillguys.nebulazone.application.products.dto.request.UpdateProductRequest;
import nbc.chillguys.nebulazone.application.products.dto.response.DeleteProductResponse;
import nbc.chillguys.nebulazone.application.products.dto.response.ProductResponse;
import nbc.chillguys.nebulazone.application.products.dto.response.PurchaseProductResponse;
import nbc.chillguys.nebulazone.application.products.dto.response.SearchProductResponse;
import nbc.chillguys.nebulazone.domain.auction.dto.AuctionCreateCommand;
import nbc.chillguys.nebulazone.domain.auction.entity.Auction;
import nbc.chillguys.nebulazone.domain.auction.service.AuctionDomainService;
import nbc.chillguys.nebulazone.domain.auth.vo.AuthUser;
import nbc.chillguys.nebulazone.domain.catalog.entity.Catalog;
import nbc.chillguys.nebulazone.domain.catalog.service.CatalogDomainService;
import nbc.chillguys.nebulazone.domain.products.dto.ChangeToAuctionTypeCommand;
import nbc.chillguys.nebulazone.domain.products.dto.ProductCreateCommand;
import nbc.chillguys.nebulazone.domain.products.dto.ProductDeleteCommand;
import nbc.chillguys.nebulazone.domain.products.dto.ProductFindQuery;
import nbc.chillguys.nebulazone.domain.products.dto.ProductPurchaseCommand;
import nbc.chillguys.nebulazone.domain.products.dto.ProductSearchCommand;
import nbc.chillguys.nebulazone.domain.products.dto.ProductUpdateCommand;
import nbc.chillguys.nebulazone.domain.products.entity.Product;
import nbc.chillguys.nebulazone.domain.products.entity.ProductEndTime;
import nbc.chillguys.nebulazone.domain.products.entity.ProductTxMethod;
import nbc.chillguys.nebulazone.domain.products.service.ProductDomainService;
import nbc.chillguys.nebulazone.domain.products.vo.ProductDocument;
import nbc.chillguys.nebulazone.domain.transaction.dto.TransactionCreateCommand;
import nbc.chillguys.nebulazone.domain.transaction.entity.Transaction;
import nbc.chillguys.nebulazone.domain.transaction.service.TransactionDomainService;
import nbc.chillguys.nebulazone.domain.user.entity.User;
import nbc.chillguys.nebulazone.domain.user.service.UserDomainService;
import nbc.chillguys.nebulazone.infra.aws.s3.S3Service;

@Service
@RequiredArgsConstructor
public class ProductService {

	private final UserDomainService userDomainService;
	private final ProductDomainService productDomainService;
	private final AuctionDomainService auctionDomainService;
	private final TransactionDomainService transactionDomainService;
	private final AuctionSchedulerService auctionSchedulerService;
	private final CatalogDomainService catalogDomainService;
	private final S3Service s3Service;

	@Transactional
	public ProductResponse createProduct(AuthUser authUser, Long catalogId, CreateProductRequest request,
		List<MultipartFile> multipartFiles) {

		User findUser = userDomainService.findActiveUserById(authUser.getId());

		List<String> productImageUrls = multipartFiles == null
			? List.of()
			: multipartFiles.stream()
			.map(s3Service::generateUploadUrlAndUploadFile)
			.toList();

		Catalog findCatalog = catalogDomainService.getCatalogById(catalogId);

		ProductCreateCommand productCreateCommand = ProductCreateCommand.of(findUser, findCatalog, request);

		ProductEndTime productEndTime = request.getProductEndTime();

		Product createdProduct = productDomainService.createProduct(productCreateCommand, productImageUrls);

		productDomainService.saveProductToEs(createdProduct);

		if (createdProduct.getTxMethod() == ProductTxMethod.AUCTION) {
			AuctionCreateCommand auctionCreateCommand = AuctionCreateCommand.of(createdProduct, productEndTime);
			Auction savedAuction = auctionDomainService.createAuction(auctionCreateCommand);
			auctionSchedulerService.autoAuctionEndSchedule(savedAuction, createdProduct.getId());
		}

		return ProductResponse.from(createdProduct, productEndTime);
	}

	public ProductResponse updateProduct(
		Long userId,
		Long catalogId,
		Long productId,
		UpdateProductRequest request,
		List<MultipartFile> imageFiles
	) {
		User user = userDomainService.findActiveUserById(userId);
		Product product = productDomainService.findActiveProductById(productId);
		Catalog catalog = catalogDomainService.getCatalogById(catalogId);

		List<String> imageUrls = new ArrayList<>(request.remainImageUrls());
		boolean hasImage = imageFiles != null && !imageFiles.isEmpty();
		if (hasImage) {
			List<String> newImageUrls = imageFiles.stream()
				.map(s3Service::generateUploadUrlAndUploadFile)
				.toList();
			imageUrls.addAll(newImageUrls);

			product.getProductImages().stream()
				.filter(productImage -> !imageUrls.contains(productImage.getUrl()))
				.forEach((productImage) -> s3Service.generateDeleteUrlAndDeleteFile(productImage.getUrl()));
		}

		ProductUpdateCommand command = request.toCommand(user, catalog, productId, imageUrls);
		Product updatedProduct = productDomainService.updateProduct(command);

		productDomainService.saveProductToEs(updatedProduct);

		return ProductResponse.from(updatedProduct);
	}

	@Transactional
	public ProductResponse changeToAuctionType(
		Long userId,
		Long catalogId,
		Long productId,
		ChangeToAuctionTypeRequest request
	) {
		User user = userDomainService.findActiveUserById(userId);
		Catalog catalog = catalogDomainService.getCatalogById(catalogId);

		ChangeToAuctionTypeCommand command = request.toCommand(user, catalog, productId);
		Product product = productDomainService.changeToAuctionType(command);

		productDomainService.saveProductToEs(product);

		auctionDomainService.createAuction(AuctionCreateCommand.of(product, request.getProductEndTime()));

		return ProductResponse.from(product, request.getProductEndTime());
	}

	@Transactional
	public DeleteProductResponse deleteProduct(Long userId, Long catalogId, Long productId) {
		User user = userDomainService.findActiveUserById(userId);
		Catalog catalog = catalogDomainService.getCatalogById(catalogId);

		ProductDeleteCommand command = ProductDeleteCommand.of(user, catalog, productId);
		Product product = productDomainService.deleteProduct(command);

		productDomainService.deleteProductFromEs(productId);

		if (Objects.equals(product.getTxMethod(), ProductTxMethod.AUCTION)) {
			Auction auction = auctionDomainService.findAuctionByProductId(productId);
			auction.delete();
		}

		return DeleteProductResponse.from(productId);
	}

	@Transactional
	public PurchaseProductResponse purchaseProduct(Long userId, Long catalogId, Long productId) {
		User user = userDomainService.findActiveUserById(userId);
		Product product = productDomainService.findAvailableProductById(productId);
		Catalog catalog = catalogDomainService.getCatalogById(catalogId);

		user.usePoint(product.getPrice());

		ProductPurchaseCommand command = ProductPurchaseCommand.of(user, catalog, productId);
		productDomainService.purchaseProduct(command);

		TransactionCreateCommand txCreateCommand
			= TransactionCreateCommand.of(user, product, product.getTxMethod().name(), product.getPrice());
		Transaction tx = transactionDomainService.createTransaction(txCreateCommand);

		return PurchaseProductResponse.from(tx);
	}

	public Page<SearchProductResponse> searchProduct(String productName, ProductTxMethod txMethod, Long priceFrom,
		Long priceTo, int page, int size) {
		ProductSearchCommand productSearchCommand = ProductSearchCommand.of(productName, txMethod, priceFrom,
			priceTo, page, size);

		Page<ProductDocument> productDocuments = productDomainService.searchProduct(productSearchCommand);

		return productDocuments.map(SearchProductResponse::from);
	}

	public ProductResponse getProduct(Long catalogId, Long productId) {
		Catalog catalog = catalogDomainService.getCatalogById(catalogId);

		ProductFindQuery query = ProductFindQuery.of(catalog.getId(), productId);
		Product product = productDomainService.getProductByIdWithUserAndImages(query);

		return ProductResponse.from(product);
	}
}
