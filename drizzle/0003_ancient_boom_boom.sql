CREATE TABLE `statementBatches` (
	`id` int AUTO_INCREMENT NOT NULL,
	`provider` varchar(64) NOT NULL,
	`fileKey` text NOT NULL,
	`fileHash` varchar(128) NOT NULL,
	`rowCount` int NOT NULL DEFAULT 0,
	`importedBy` int NOT NULL,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	CONSTRAINT `statementBatches_id` PRIMARY KEY(`id`),
	CONSTRAINT `statementBatches_fileHash_unique` UNIQUE(`fileHash`)
);
--> statement-breakpoint
CREATE TABLE `statementRows` (
	`id` int AUTO_INCREMENT NOT NULL,
	`batchId` int NOT NULL,
	`externalId` varchar(160) NOT NULL,
	`transactionDate` varchar(64) NOT NULL,
	`amount` varchar(32) NOT NULL,
	`direction` enum('CREDIT','DEBIT') NOT NULL,
	`account` varchar(128),
	`reference` varchar(160),
	`description` text,
	`verificationStatus` enum('UNMATCHED','AMBIGUOUS','VERIFIED','EXCEPTION') NOT NULL DEFAULT 'UNMATCHED',
	`transactionId` int,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	CONSTRAINT `statementRows_id` PRIMARY KEY(`id`)
);
