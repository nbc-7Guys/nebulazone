package nbc.chillguys.nebulazone.application.product.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import nbc.chillguys.nebulazone.application.auction.service.AuctionSchedulerService;
import nbc.chillguys.nebulazone.application.product.dto.request.ChangeToAuctionTypeRequest;
import nbc.chillguys.nebulazone.application.product.dto.request.CreateProductRequest;
import nbc.chillguys.nebulazone.application.product.dto.request.UpdateProductRequest;
import nbc.chillguys.nebulazone.application.product.dto.response.DeleteProductResponse;
import nbc.chillguys.nebulazone.application.product.dto.response.ProductResponse;
import nbc.chillguys.nebulazone.application.product.dto.response.PurchaseProductResponse;
import nbc.chillguys.nebulazone.application.product.dto.response.SearchProductResponse;
import nbc.chillguys.nebulazone.domain.auction.dto.AuctionCreateCommand;
import nbc.chillguys.nebulazone.domain.auction.entity.Auction;
import nbc.chillguys.nebulazone.domain.auction.service.AuctionDomainService;
import nbc.chillguys.nebulazone.domain.catalog.entity.Catalog;
import nbc.chillguys.nebulazone.domain.catalog.service.CatalogDomainService;
import nbc.chillguys.nebulazone.domain.product.dto.ChangeToAuctionTypeCommand;
import nbc.chillguys.nebulazone.domain.product.dto.ProductCreateCommand;
import nbc.chillguys.nebulazone.domain.product.dto.ProductDeleteCommand;
import nbc.chillguys.nebulazone.domain.product.dto.ProductFindQuery;
import nbc.chillguys.nebulazone.domain.product.dto.ProductPurchaseCommand;
import nbc.chillguys.nebulazone.domain.product.dto.ProductSearchCommand;
import nbc.chillguys.nebulazone.domain.product.dto.ProductUpdateCommand;
import nbc.chillguys.nebulazone.domain.product.entity.Product;
import nbc.chillguys.nebulazone.domain.product.entity.ProductEndTime;
import nbc.chillguys.nebulazone.domain.product.entity.ProductTxMethod;
import nbc.chillguys.nebulazone.domain.product.service.ProductDomainService;
import nbc.chillguys.nebulazone.domain.product.vo.ProductDocument;
import nbc.chillguys.nebulazone.domain.transaction.dto.TransactionCreateCommand;
import nbc.chillguys.nebulazone.domain.transaction.entity.Transaction;
import nbc.chillguys.nebulazone.domain.transaction.entity.UserType;
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
	public ProductResponse createProduct(User user, Long catalogId, CreateProductRequest request,
		List<MultipartFile> multipartFiles) {

		List<String> productImageUrls = multipartFiles == null
			? List.of()
			: multipartFiles.stream()
			.map(s3Service::generateUploadUrlAndUploadFile)
			.toList();

		Catalog findCatalog = catalogDomainService.getCatalogById(catalogId);

		ProductCreateCommand productCreateCommand = ProductCreateCommand.of(user, findCatalog, request);

		ProductEndTime productEndTime = request.getProductEndTime();

		Product createdProduct = productDomainService.createProduct(productCreateCommand, productImageUrls);

		if (createdProduct.getTxMethod() == ProductTxMethod.AUCTION) {
			AuctionCreateCommand auctionCreateCommand = AuctionCreateCommand.of(createdProduct, productEndTime);
			Auction savedAuction = auctionDomainService.createAuction(auctionCreateCommand);
			auctionSchedulerService.autoAuctionEndSchedule(savedAuction, createdProduct.getId());
			createdProduct.updateAuctionId(savedAuction.getId());
		}

		productDomainService.saveProductToEs(createdProduct);

		return ProductResponse.from(createdProduct, productEndTime);
	}

	public ProductResponse updateProduct(
		User user,
		Long catalogId,
		Long productId,
		UpdateProductRequest request,
		List<MultipartFile> imageFiles
	) {
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
		User user,
		Long catalogId,
		Long productId,
		ChangeToAuctionTypeRequest request
	) {
		Catalog catalog = catalogDomainService.getCatalogById(catalogId);

		ChangeToAuctionTypeCommand command = request.toCommand(user, catalog, productId);
		Product product = productDomainService.changeToAuctionType(command);

		productDomainService.saveProductToEs(product);

		auctionDomainService.createAuction(AuctionCreateCommand.of(product, request.getProductEndTime()));

		return ProductResponse.from(product, request.getProductEndTime());
	}

	@Transactional
	public DeleteProductResponse deleteProduct(User user, Long catalogId, Long productId) {
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
	public PurchaseProductResponse purchaseProduct(User loggedInUser, Long catalogId, Long productId) {
		User user = userDomainService.findActiveUserById(loggedInUser.getId());
		Product product = productDomainService.findAvailableProductById(productId);
		Catalog catalog = catalogDomainService.getCatalogById(catalogId);

		product.validPurchasable(user.getId());
		user.usePoint(product.getPrice());

		ProductPurchaseCommand command = ProductPurchaseCommand.of(user, catalog, productId);
		productDomainService.purchaseProduct(command);

		productDomainService.saveProductToEs(product);

		TransactionCreateCommand buyerTxCreateCommand
			= TransactionCreateCommand.of(user, UserType.BUYER, product, product.getTxMethod().name(),
			product.getPrice());
		Transaction buyerTx = transactionDomainService.createTransaction(buyerTxCreateCommand);
		TransactionCreateCommand sellerTxCreateCommand
			= TransactionCreateCommand.of(product.getSeller(), UserType.SELLER, product, product.getTxMethod().name(),
			product.getPrice());
		Transaction sellerTx = transactionDomainService.createTransaction(sellerTxCreateCommand);

		return PurchaseProductResponse.from(buyerTx, sellerTx);
	}

	public Page<SearchProductResponse> searchProduct(String productName, String sellerNickname,
		ProductTxMethod txMethod, Long priceFrom,
		Long priceTo, int page, int size) {
		ProductSearchCommand productSearchCommand = ProductSearchCommand.of(productName, sellerNickname, txMethod,
			priceFrom, priceTo, page, size);

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
