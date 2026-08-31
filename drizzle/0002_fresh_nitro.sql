CREATE TABLE `agentAccounts` (
	`id` int AUTO_INCREMENT NOT NULL,
	`label` varchar(160) NOT NULL,
	`accountNumber` varchar(128) NOT NULL,
	`method` varchar(64) NOT NULL,
	`active` int NOT NULL DEFAULT 1,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	CONSTRAINT `agentAccounts_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `appSettings` (
	`id` int AUTO_INCREMENT NOT NULL,
	`whatsappNumber` varchar(40) NOT NULL,
	`minTransaction` int NOT NULL,
	`maxTransaction` int NOT NULL,
	`promoCode` varchar(64) NOT NULL,
	CONSTRAINT `appSettings_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
ALTER TABLE `transactions` ADD `securityCode` varchar(128);--> statement-breakpoint
ALTER TABLE `transactions` ADD `bank` varchar(160);--> statement-breakpoint
ALTER TABLE `transactions` ADD `accountNumber` varchar(128);--> statement-breakpoint
ALTER TABLE `transactions` ADD `contactNumber` varchar(40);--> statement-breakpoint
ALTER TABLE `users` ADD `playerId` varchar(64);