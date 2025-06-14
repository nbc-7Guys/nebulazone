package nbc.chillguys.nebulazone.domain.post.dto;

import nbc.chillguys.nebulazone.domain.post.entity.PostType;

public record PostAdminSearchQueryCommand(
	String keyword,
	PostType type,
	Boolean includeDeleted
) {
}
